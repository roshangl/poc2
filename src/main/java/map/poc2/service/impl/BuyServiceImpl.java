package map.poc2.service.impl;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import map.poc2.constants.CacheType;
import map.poc2.constants.UtilityArray;
import map.poc2.model.*;
import map.poc2.service.BuyService;
import map.poc2.util.POC2Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.stream.IntStream;

@Service
public class BuyServiceImpl implements BuyService {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public ExecutionTimeResult initializeModel(IntializeArgs intializeArgs) {
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        cache.clear();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int customerChoiceCount = intializeArgs.getCustomerChoice();
        int clusterCount = intializeArgs.getCluster();
        int weekCount = intializeArgs.getWeek();
        IntStream.range(0, intializeArgs.getMeasure()).parallel().forEach(measureIndex -> {
            double[][][] value = new double[customerChoiceCount][clusterCount][weekCount];
            IntStream.range(0, value.length).parallel().forEach(i -> {
                IntStream.range(0, value[i].length).parallel().forEach(j -> {
                    IntStream.range(0, value[i][j].length).parallel().forEach(k -> {
                        value[i][j][k] = i + j + k + measureIndex;
                    });
                });
            });
            cache.put("M" + measureIndex, value);
        });

        stopWatch.stop();
        ExecutionTimeResult executionTimeResult = new ExecutionTimeResult();
        executionTimeResult.setDurationSec(stopWatch.getTotalTimeSeconds());
        return executionTimeResult;
    }

    @Override
    public ExecutionTimeResult setModel(SetArgs setArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(CacheType.HAZELCAST_CACHE.getValue());
        IMap<String, double[][][]> measureGroupsCache = hazelcastInstance.getMap(CacheType.BUY_SHEET.getValue());
        int[] measureRange1 = POC2Utility.identifyMeasureRange(setArgs.getMeasure(), measureGroupsCache.keySet().size());

        List<Double> allOldValues = new ArrayList<>();
        List<Integer> allNewValues = new ArrayList<>();
        Map<Integer, Double> allNewDecimalValuesMap = new HashMap<>();
        List<Double> allNewDecimalValues = new ArrayList<>();
            for (int x = 0; x < measureRange1.length; x++) {
            double[][][] value = measureGroupsCache.get("M" + measureRange1[x]);
            int[] customerChoiceRange = POC2Utility.identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
            int[] clusterRange = POC2Utility.identifyClusterRange(setArgs.getCluster(), value[0].length);
            int[] weekRange = POC2Utility.identifyWeekRange(setArgs.getWeek(), value[0][0].length);

            for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
                for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                    for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                        allOldValues.add(value[i][j][k]);
                    }
                }
            }
        }

        double sumOfAllValues = allOldValues.parallelStream().mapToDouble(i -> i).sum();
        double unitValue = setArgs.getValue() / sumOfAllValues;

        for (int i = 0; i < allOldValues.size(); i++) {
            double temp = allOldValues.get(i) * unitValue;
            allNewDecimalValues.add(temp);
            allNewDecimalValuesMap.put(i, temp);
            allNewValues.add((int) Math.floor(temp));
        }

        int sumOfAllNewValues = allNewValues.parallelStream().mapToInt(i -> i).sum();
        int difference = (int) setArgs.getValue() - sumOfAllNewValues;
        allNewDecimalValues.sort((new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                String[] s1 = String.valueOf(o1).split("\\.");
                String[] s2 = String.valueOf(o2).split("\\.");
                if (Long.parseLong(s1[1]) < Long.parseLong(s2[1])) {
                    return 1;
                } else if (Long.parseLong(s1[1]) > Long.parseLong(s2[1])) {
                    return -1;
                }
                return 0;
            }
        }));

        for (int i = 0; i < difference; i++) {
            Double aDouble = allNewDecimalValues.get(i);
            Integer key = allNewDecimalValuesMap.keySet().stream().filter(k -> allNewDecimalValuesMap.get(k).equals(aDouble)).findFirst().orElse(null);
            allNewValues.set(key, allNewValues.get(key) + 1);
            allNewDecimalValuesMap.remove(key);
        }

        for (int x = 0, z = 0; x < measureRange1.length; x++) {
            double[][][] value = measureGroupsCache.get("M" + measureRange1[x]);
            int[] customerChoiceRange = POC2Utility.identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
            int[] clusterRange = POC2Utility.identifyClusterRange(setArgs.getCluster(), value[0].length);
            int[] weekRange = POC2Utility.identifyWeekRange(setArgs.getWeek(), value[0][0].length);

            for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
                for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                    for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                        value[i][j][k] = allNewValues.get(z++);
                    }
                }
            }
            measureGroupsCache.put("M" + measureRange1[x], value);
        }

        stopWatch.stop();
        ExecutionTimeResult executionTimeResult = new ExecutionTimeResult();
        executionTimeResult.setDurationSec(stopWatch.getTotalTimeSeconds());
        return executionTimeResult;
    }

    @Override
    public GetResult getModel(GetArgs getArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int customerChoiceLength = getArgs.getCustomerChoice().length;
        int clusterLength = getArgs.getCluster().length;
        int weekLength = getArgs.getWeek().length;
        int measureLength = getArgs.getMeasure().length;

        double result[][][][] = new double[customerChoiceLength][clusterLength][weekLength][measureLength];

        for (int i = 0; i < customerChoiceLength; i++) {
            for (int j = 0; j < clusterLength; j++) {
                for (int k = 0; k < weekLength; k++) {
                    for (int l = 0; l < measureLength; l++) {
                        result[i][j][k][l] = POC2Utility.getTotal(getArgs.getCustomerChoice()[i], getArgs.getCluster()[j], getArgs.getWeek()[k], getArgs.getMeasure()[l]);
                    }
                }
            }
        }

        stopWatch.stop();
        GetResult getResult = new GetResult();
        getResult.setValue(result);
        getResult.setDurationSec(stopWatch.getTotalTimeSeconds());
        return getResult;
    }

    @Override
    public ExecutionTimeResult applyRules() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(CacheType.HAZELCAST_CACHE.getValue());
        IMap<String, double[][][]> measureGroupsCache = hazelcastInstance.getMap(CacheType.BUY_SHEET.getValue());
        Map<Integer, Map<String, double[][][]>> measureGroups = POC2Utility.createMeasureGroups(measureGroupsCache);

        measureGroups.keySet().parallelStream().forEach(key -> {

            Map<String, double[][][]> measureGroup = measureGroups.get(key);
            int measureGroupSize = measureGroup.keySet().size();
            int[] measureRange = UtilityArray.measureArray[key].clone();

            if (key == measureGroups.keySet().size() - 1) {
                measureRange[1] = measureGroupSize % 4 == 0 ? measureRange[1] : measureRange[0] + measureGroupSize - 1;
            }

            List<String> measureKeys = new ArrayList<>();
            List<double[][][]> measureCubes = new ArrayList<>();
            int z = 0;
            for (int i = measureRange[0]; i <= measureRange[1]; i++) {
                String measureKey = "M" + (measureRange[0] + z++);
                measureKeys.add(measureKey);
                measureCubes.add(measureGroup.get(measureKey));
            }

            for (int i = 0; i < measureCubes.get(0).length; i++) {
                for (int j = 0; j < measureCubes.get(0)[i].length; j++) {
                    for (int k = 0; k < measureCubes.get(0)[i][j].length; k++) {
                        if (measureCubes.size() > 1) {
                            measureCubes.get(1)[i][j][k] = measureCubes.get(0)[i][j][k] * 5;
                            if (measureCubes.size() > 3)
                                if (k == 0) {
                                    measureCubes.get(3)[i][j][k] = measureCubes.get(2)[i][j][k] - measureCubes.get(0)[i][j][k];
                                } else {
                                    measureCubes.get(3)[i][j][k] = measureCubes.get(3)[i][j][k - 1] + measureCubes.get(2)[i][j][k] - measureCubes.get(0)[i][j][k];
                                }
                        }
                    }
                }
            }
            for (int i = 0; i < measureCubes.size(); i++) {
                measureGroup.put(measureKeys.get(i), measureCubes.get(i));
            }
        });

        measureGroupsCache.clear();
        measureGroups.keySet().stream().forEach(key -> {
            Map<String, double[][][]> tempMap = measureGroups.get(key);
            tempMap.keySet().stream().forEach(tempKey -> measureGroupsCache.put(tempKey, tempMap.get(tempKey)));
        });

        stopWatch.stop();
        ExecutionTimeResult executionTimeResult = new ExecutionTimeResult();
        executionTimeResult.setDurationSec(stopWatch.getTotalTimeSeconds());
        return executionTimeResult;
    }


    @Override
    public GetResult getAllModel(SetArgs setArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        Cache.ValueWrapper valueWrapper = cache.get(setArgs.getMeasure().toUpperCase());
        double[][][] value = (double[][][]) valueWrapper.get();

        int customerChoiceDisplayLimit = POC2Utility.identifyCustomerChoiceDisplayLimit(setArgs.getCustomerChoice(), value.length);
        int clusterDisplayLimit = POC2Utility.identifyClusterDisplayLimit(setArgs.getCluster(), value[0].length);
        int weekDisplayLimit = POC2Utility.identifyWeekDisplayLimit(setArgs.getWeek(), value[0][0].length);
        double[][][][] result = new double[customerChoiceDisplayLimit][clusterDisplayLimit][weekDisplayLimit][1];

        int[] customerChoiceRange = POC2Utility.identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
        int[] clusterRange = POC2Utility.identifyClusterRange(setArgs.getCluster(), value[0].length);
        int[] weekRange = POC2Utility.identifyWeekRange(setArgs.getWeek(), value[0][0].length);

        List<Double> allValues = new ArrayList<>();
        for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
            for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                    allValues.add(value[i][j][k]);
                }
            }
        }
        int z = 0;
        for (int i = 0; i < customerChoiceDisplayLimit; i++) {
            for (int j = 0; j < clusterDisplayLimit; j++) {
                for (int k = 0; k < weekDisplayLimit; k++) {
                    result[i][j][k][0] = allValues.get(z++);
                }
            }
        }
        stopWatch.stop();
        GetResult getResult = new GetResult();
        getResult.setValue(result);
        getResult.setDurationSec(stopWatch.getTotalTimeSeconds());
        return getResult;
    }
}
