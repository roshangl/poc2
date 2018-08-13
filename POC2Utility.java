package map.poc2.util;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import map.poc2.constants.CacheType;
import map.poc2.constants.UtilityArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class POC2Utility {

    private POC2Utility() {
        //intentionally make private constructor
    }

    public static Map<Integer, Map<String, double[][][]>> createMeasureGroups(IMap<String, double[][][]> measureGroupsCache) {
        int measureCubeSize = measureGroupsCache.keySet().size();
        int totalMeasureGroups = 0;
        if (measureCubeSize % 4 == 0) {
            totalMeasureGroups = measureCubeSize / 4;
        } else {
            totalMeasureGroups = (measureCubeSize / 4) + 1;
        }
        Map<Integer, Map<String, double[][][]>> measureGroups = new HashMap<>();
        for (int i = 0; i < totalMeasureGroups; i++) {
            int[] measureRange = UtilityArray.measureArray[i].clone();
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

    public static double getTotal(String customerChoice, String cluster, String week, String measure) {
        HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(CacheType.HAZELCAST_CACHE.getValue());
        IMap<String, double[][][]> measureGroupsCache = hazelcastInstance.getMap(CacheType.BUY_SHEET.getValue());
        int[] measureRange = identifyMeasureRange(measure, measureGroupsCache.keySet().size());
        List<Double> allValues = new ArrayList<>();

        for (int x = 0; x < measureRange.length; x++) {
            double[][][] value = measureGroupsCache.get("M" + measureRange[x]);
            int[] customerChoiceRange = identifyCustomerChoiceRange(customerChoice, value.length);
            int[] clusterRange = identifyClusterRange(cluster, value[0].length);
            int[] weekRange = identifyWeekRange(week, value[0][0].length);
            for (int i = customerChoiceRange[0]; i <= customerChoiceRange[1]; i++) {
                for (int j = clusterRange[0]; j <= clusterRange[1]; j++) {
                    for (int k = weekRange[0]; k <= weekRange[1]; k++) {
                        allValues.add(value[i][j][k]);
                    }
                }
            }
        }
        return allValues.parallelStream().mapToDouble(i -> i).sum();
    }

    public static int[] identifyMeasureRange(String measure, int size) {
        if (measure.toLowerCase().startsWith("m")) {
            int numericValue = Integer.parseInt(measure.replaceAll("\\D", ""));
            int[] range = new int[1];
            range[0] = numericValue;
            return range;
        } else if (measure.equalsIgnoreCase("salesU")) {
            List<Integer> measureList = new ArrayList<>();
            for (int i = 0; i < UtilityArray.salesUArray[0].length; i++) {
                if (UtilityArray.salesUArray[0][i] <= size - 1) {
                    measureList.add(UtilityArray.salesUArray[0][i]);
                } else if (UtilityArray.salesUArray[0][i] > size - 1 && UtilityArray.salesUArray[0][i - 1] < size - 1 && size % 4 == 1) {
                    measureList.add(size - 1);
                }
            }
            int[] range = measureList.stream().mapToInt(i -> i).toArray();
            return range;
        } else if (measure.equalsIgnoreCase("salesD")) {
            List<Integer> measureList = new ArrayList<>();
            for (int i = 0; i < UtilityArray.salesDArray[0].length; i++) {
                if (UtilityArray.salesDArray[0][i] <= size - 1) {
                    measureList.add(UtilityArray.salesDArray[0][i]);
                } else if (UtilityArray.salesDArray[0][i] > size - 1 && UtilityArray.salesDArray[0][i - 1] < size - 1 && size % 4 == 2) {
                    measureList.add(size - 1);
                }
            }
            int[] range = measureList.stream().mapToInt(i -> i).toArray();
            return range;
        } else if (measure.equalsIgnoreCase("receiptU")) {
            List<Integer> measureList = new ArrayList<>();
            for (int i = 0; i < UtilityArray.receiptUArray[0].length; i++) {
                if (UtilityArray.receiptUArray[0][i] <= size - 1) {
                    measureList.add(UtilityArray.receiptUArray[0][i]);
                } else if (UtilityArray.receiptUArray[0][i] > size - 1 && UtilityArray.receiptUArray[0][i - 1] < size - 1 && size % 4 == 3) {
                    measureList.add(size - 1);
                }
            }
            int[] range = measureList.stream().mapToInt(i -> i).toArray();
            return range;
        } else if (measure.equalsIgnoreCase("other")) {
            List<Integer> measureList = new ArrayList<>();
            for (int i = 0; i < UtilityArray.otherArray[0].length; i++) {
                if (UtilityArray.otherArray[0][i] <= size - 1) {
                    measureList.add(UtilityArray.otherArray[0][i]);
                } else if (UtilityArray.otherArray[0][i] > size - 1 && UtilityArray.otherArray[0][i - 1] < size - 1 && size % 4 == 0) {
                    measureList.add(size - 1);
                }
            }
            int[] range = measureList.stream().mapToInt(i -> i).toArray();
            return range;
        }
        return null;
    }

    public static int[] identifyWeekRange(String week, int noOfWeeks) {
        if (week.toLowerCase().startsWith("w")) {
            int numericValue = Integer.parseInt(week.replaceAll("\\D", ""));
            int[] range = new int[2];
            range[0] = numericValue;
            range[1] = numericValue;
            return range;
        } else if (week.toLowerCase().startsWith("m")) {
            int numericValue = Integer.parseInt(week.replaceAll("\\D", ""));
            int[] range = UtilityArray.monthArray[numericValue].clone();
            range[1] = range[1] > noOfWeeks - 1 ? noOfWeeks - 1 : range[1];
            return range;
        } else if (week.toLowerCase().startsWith("q")) {
            int numericValue = Integer.parseInt(week.replaceAll("\\D", ""));
            int[] range = UtilityArray.quarterArray[numericValue].clone();
            range[1] = range[1] > noOfWeeks - 1 ? noOfWeeks - 1 : range[1];
            return range;
        } else if (week.toLowerCase().startsWith("s")) {
            int numericValue = Integer.parseInt(week.replaceAll("\\D", ""));
            int[] range = UtilityArray.seasonArray[numericValue].clone();
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

    public static int identifyWeekDisplayLimit(String week, int noOfWeeks) {
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

    public static int[] identifyClusterRange(String cluster, int noOfClusters) {
        if (cluster.toLowerCase().startsWith("c")) {
            int numericValue = Integer.parseInt(cluster.replaceAll("\\D", ""));
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
                int numericValue = Integer.parseInt(cluster.replaceAll("\\D", ""));
                int[] range = UtilityArray.tierArray[numericValue].clone();
                range[1] = range[1] > noOfClusters - 1 ? noOfClusters - 1 : range[1];
                return range;
            }
        } else if (cluster.toLowerCase().startsWith("d")) {
            return UtilityArray.digitalArray[0];
        } else if (cluster.toLowerCase().startsWith("s")) {
            int[] range = UtilityArray.storeArray[0].clone();
            range[1] = range[1] > noOfClusters - 1 ? noOfClusters - 1 : range[1];
            return range;
        }
        return null;
    }

    public static int identifyClusterDisplayLimit(String cluster, int noOfClusters) {
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

    public static int[] identifyCustomerChoiceRange(String customerChoice, int noOfCustomerChoices) {
        if (customerChoice.toLowerCase().startsWith("c")) {
            int numericValue = Integer.parseInt(customerChoice.replaceAll("\\D", ""));
            int[] range = new int[2];
            range[0] = numericValue;
            range[1] = numericValue;
            return range;
        } else if (customerChoice.toLowerCase().startsWith("s")) {
            int numericValue = Integer.parseInt(customerChoice.replaceAll("\\D", ""));
            int[] range = UtilityArray.styleArray[numericValue].clone();
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

    public static int identifyCustomerChoiceDisplayLimit(String customerChoice, int noOfCustomerChoices) {
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
