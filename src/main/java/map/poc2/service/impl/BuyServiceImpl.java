package map.poc2.service.impl;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import map.poc2.constants.CacheType;
import map.poc2.model.*;
import map.poc2.service.BuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class BuyServiceImpl implements BuyService {

    @Autowired
    private CacheManager cacheManager;

    private final static int[][] styleArray = {{0, 4}, {5, 9}, {10, 14}, {15, 19}, {20, 24}, {25, 29}, {30, 34}, {35, 39}, {40, 44}, {45, 49}, {50, 54},
            {55, 59}, {60, 64}, {65, 69}, {70, 74}, {75, 79}, {80, 84}, {85, 89}, {90, 94}, {95, 99}, {100, 104}, {105, 109}, {110, 114}, {115, 119},
            {120, 124}, {125, 129}, {130, 134}, {135, 139}, {140, 144}, {145, 149}, {150, 154}, {155, 159}, {160, 164}, {165, 169}, {170, 174}, {175, 179},
            {180, 184}, {185, 189}, {190, 194}, {195, 199}};

    private final static int[][] tierArray = {{0, 3}, {4, 7}, {8, 11}, {12, 15}, {16, 19}, {20, 23}, {24, 27}, {28, 31}, {32, 35}, {36, 39}, {40, 43},
            {44, 47}, {48, 51}, {52, 55}, {56, 59}};

    private final static int[][] monthArray = {{0, 3}, {4, 7}, {8, 11}, {12, 15}, {16, 19}, {20, 23}, {24, 27}, {28, 31}, {32, 35}, {36, 39}, {40, 43}, {44, 47}};
    private final static int[][] quarterArray = {{0, 11}, {12, 23}, {24, 35}, {36, 47}};
    private final static int[][] seasonArray = {{0, 23}, {24, 47}};
    private final static int[][] measureArray = {{0, 3}, {4, 7}, {8, 11}, {12, 15}, {16, 19}, {20, 23}, {24, 27}, {28, 31}, {32, 35}, {36, 39}, {40, 43},
            {44, 47}, {48, 51}, {52, 55}, {56, 59}, {60, 63}, {64, 67}, {68, 71}, {72, 75}, {76, 79}, {80, 83}, {84, 87}, {88, 91}, {92, 95}, {96, 99}};

    @Override
    public ExecutionTimeResult initializeModel(IntializeArgs intializeArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        cache.clear();
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
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        Cache.ValueWrapper valueWrapper = cache.get(setArgs.getMeasure().toUpperCase());
        double[][][] value = (double[][][]) valueWrapper.get();

        int[] customerChoiceRange = identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
        int[] clusterRange = identifyClusterRange(setArgs.getCluster(), value[0].length);
        int[] weekRange = identifyWeekRange(setArgs.getWeek(), value[0][0].length);

        List<Double> allValues = new ArrayList<>();

        for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
            for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                    allValues.add(value[i][j][k]);
                }
            }
        }

        double sumOfAllValues = allValues.parallelStream().mapToDouble(i -> i).sum();
        double unitValue = setArgs.getValue() / sumOfAllValues;
        int z = 0;

        for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
            for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                    value[i][j][k] = allValues.get(z++) * unitValue;
                }
            }
        }

        cache.put(setArgs.getMeasure().toUpperCase(), value);
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
                        result[i][j][k][l] = getTotal(getArgs.getCustomerChoice()[i], getArgs.getCluster()[j], getArgs.getWeek()[k], getArgs.getMeasure()[l]);
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

        Map<Integer, Map<String, double[][][]>> measureGroups = createMeasureGroups(measureGroupsCache);

        measureGroups.keySet().parallelStream().forEach(key -> {

            Map<String, double[][][]> measureGroup = measureGroups.get(key);
            int measureGroupSize = measureGroup.keySet().size();
            int[] measureRange = measureArray[key].clone();

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

    private Map<Integer, Map<String, double[][][]>> createMeasureGroups(IMap<String, double[][][]> measureGroupsCache) {
        int measureCubeSize = measureGroupsCache.keySet().size();
        int totalMeasureGroups = 0;
        if (measureCubeSize % 4 == 0) {
            totalMeasureGroups = measureCubeSize / 4;
        } else {
            totalMeasureGroups = (measureCubeSize / 4) + 1;
        }
        Map<Integer, Map<String, double[][][]>> measureGroups = new HashMap<>();
        for (int i = 0; i < totalMeasureGroups; i++) {
            int[] measureRange = measureArray[i].clone();
            Map<String, double[][][]> tempMap = new HashMap<>();
            if (i == totalMeasureGroups - 1) {
                measureRange[1] = measureCubeSize % 4 == 0 ? measureRange[1] : measureCubeSize - 1;
            }
            for (int j = measureRange[0]; j <= measureRange[1]; j++) {
                String key = "M" + j;
                tempMap.put(key, measureGroupsCache.get(key));
            }
            measureGroups.put(i, tempMap);

        }
        return measureGroups;
    }

    private double getTotal(String customerChoice, String cluster, String week, String measure) {
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        Cache.ValueWrapper valueWrapper = cache.get(measure.toUpperCase());
        double[][][] value = (double[][][]) valueWrapper.get();

        int[] customerChoiceRange = identifyCustomerChoiceRange(customerChoice, value.length);
        int[] clusterRange = identifyClusterRange(cluster, value[0].length);
        int[] weekRange = identifyWeekRange(week, value[0][0].length);

        List<Double> allValues = new ArrayList<>();
        for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
            for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                    allValues.add(value[i][j][k]);
                }
            }
        }
        return allValues.parallelStream().mapToDouble(i -> i).sum();
    }

    @Override
    public GetResult getAllModel(SetArgs setArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        Cache.ValueWrapper valueWrapper = cache.get(setArgs.getMeasure().toUpperCase());
        double[][][] value = (double[][][]) valueWrapper.get();

        int customerChoiceDisplayLimit = identifyCustomerChoiceDisplayLimit(setArgs.getCustomerChoice(), value.length);
        int clusterDisplayLimit = identifyClusterDisplayLimit(setArgs.getCluster(), value[0].length);
        int weekDisplayLimit = identifyWeekDisplayLimit(setArgs.getWeek(), value[0][0].length);
        double[][][][] result = new double[customerChoiceDisplayLimit][clusterDisplayLimit][weekDisplayLimit][1];

        int[] customerChoiceRange = identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
        int[] clusterRange = identifyClusterRange(setArgs.getCluster(), value[0].length);
        int[] weekRange = identifyWeekRange(setArgs.getWeek(), value[0][0].length);

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

    public GetResult getModelApproach2(SetArgs setArgs) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Cache cache = cacheManager.getCache(CacheType.BUY_SHEET.getValue());
        Cache.ValueWrapper valueWrapper = cache.get(setArgs.getMeasure().toUpperCase());
        double[][][] value = (double[][][]) valueWrapper.get();

        int[] customerChoiceRange = identifyCustomerChoiceRange(setArgs.getCustomerChoice(), value.length);
        int[] clusterRange = identifyClusterRange(setArgs.getCluster(), value[0].length);
        int[] weekRange = identifyWeekRange(setArgs.getWeek(), value[0][0].length);
        double[][][][] result = new double[customerChoiceRange[1]][clusterRange[1]][weekRange[1]][1];

        for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
            for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                    result[i][j][k][0] = value[i][j][k];
                }
            }
        }
        stopWatch.stop();
        GetResult getResult = new GetResult();
        getResult.setValue(result);
        getResult.setDurationSec(stopWatch.getTotalTimeMillis());
        return getResult;
    }

    private int[] identifyWeekRange(String week, int noOfWeeks) {
        int numericIndex = week.length() - 1;
        if (week.toLowerCase().startsWith("w")) {
            int numericValue = Integer.parseInt(String.valueOf(week.charAt(numericIndex)));
            int[] range = new int[2];
            range[0] = numericValue;
            range[1] = numericValue;
            return range;
        } else if (week.toLowerCase().startsWith("m")) {
            int numericValue = Integer.parseInt(String.valueOf(week.charAt(numericIndex)));
            int[] range = monthArray[numericValue].clone();
            range[1] = range[1] > noOfWeeks - 1 ? noOfWeeks - 1 : range[1];
            return range;
        } else if (week.toLowerCase().startsWith("q")) {
            int numericValue = Integer.parseInt(String.valueOf(week.charAt(numericIndex)));
            int[] range = quarterArray[numericValue].clone();
            range[1] = range[1] > noOfWeeks - 1 ? noOfWeeks - 1 : range[1];
            return range;
        } else if (week.toLowerCase().startsWith("s")) {
            int numericValue = Integer.parseInt(String.valueOf(week.charAt(numericIndex)));
            int[] range = seasonArray[numericValue].clone();
            range[1] = range[1] > noOfWeeks - 1 ? noOfWeeks - 1 : range[1];
            return range;
        } else if (week.toLowerCase().startsWith("t")) {
            int[] range = new int[2];
            range[0] = 0;
            range[1] = noOfWeeks - 1;
            return range;
        }
        return null;
    }

    private int identifyWeekDisplayLimit(String week, int noOfWeeks) {
        int numericIndex = week.length() - 1;
        if (week.toLowerCase().startsWith("w")) {
            return 1;
        } else if (week.toLowerCase().startsWith("m")) {
            return 4;
        } else if (week.toLowerCase().startsWith("q")) {
            return 12;
        } else if (week.toLowerCase().startsWith("s")) {
            return 24;
        } else if (week.toLowerCase().startsWith("t")) {
            return noOfWeeks;
        }
        return 0;
    }

    private int[] identifyClusterRange(String cluster, int noOfClusters) {
        int numericIndex = cluster.length() - 1;
        if (cluster.toLowerCase().startsWith("c")) {
            int numericValue = Integer.parseInt(String.valueOf(cluster.charAt(numericIndex)));
            int[] range = new int[2];
            range[0] = numericValue;
            range[1] = numericValue;
            return range;
        } else if (cluster.toLowerCase().startsWith("t")) {
            if (cluster.toLowerCase().endsWith("l")) {
                int[] range = new int[2];
                range[0] = 0;
                range[1] = noOfClusters - 1;
                return range;
            } else {
                int numericValue = Integer.parseInt(String.valueOf(cluster.charAt(numericIndex)));
                int[] range = tierArray[numericValue].clone();
                range[1] = range[1] > noOfClusters - 1 ? noOfClusters - 1 : range[1];
                return range;
            }
        }
        return null;
    }

    private int identifyClusterDisplayLimit(String cluster, int noOfClusters) {
        int numericIndex = cluster.length() - 1;
        if (cluster.toLowerCase().startsWith("c")) {
            return 1;
        } else if (cluster.toLowerCase().startsWith("t")) {
            if (cluster.toLowerCase().endsWith("l")) {
                return noOfClusters;
            } else {
                return 4;
            }
        }
        return 0;
    }

    private int[] identifyCustomerChoiceRange(String customerChoice, int noOfCustomerChoices) {
        int numericIndex = customerChoice.length() - 1;
        if (customerChoice.toLowerCase().startsWith("c")) {
            int numericValue = Integer.parseInt(String.valueOf(customerChoice.charAt(numericIndex)));
            int[] range = new int[2];
            range[0] = numericValue;
            range[1] = numericValue;
            return range;
        } else if (customerChoice.toLowerCase().startsWith("s")) {
            int numericValue = Integer.parseInt(String.valueOf(customerChoice.charAt(numericIndex)));
            int[] range = styleArray[numericValue].clone();
            range[1] = range[1] > noOfCustomerChoices - 1 ? noOfCustomerChoices - 1 : range[1];
            return range;
        } else if (customerChoice.toLowerCase().startsWith("t")) {
            int[] range = new int[2];
            range[0] = 0;
            range[1] = noOfCustomerChoices - 1;
            return range;
        }
        return null;
    }

    private int identifyCustomerChoiceDisplayLimit(String customerChoice, int noOfCustomerChoices) {
        int numericIndex = customerChoice.length() - 1;
        if (customerChoice.toLowerCase().startsWith("c")) {
            return 1;
        } else if (customerChoice.toLowerCase().startsWith("s")) {
            return 5;
        } else if (customerChoice.toLowerCase().startsWith("t")) {
            return noOfCustomerChoices;
        }
        return 0;
    }
}
