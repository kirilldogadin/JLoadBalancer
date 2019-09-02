package uricomputer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Вычисляет uri согласно приориетам весов
 * точность до разрядности Double
 * Использует дерево. Сложность O(log(n))
 */
public class WeightedRoundRobinTreeDoubleInterval implements UriComputer{
    private final NavigableMap<Double, List<ServerDetails>> serversByWeightNavi;
    private final ThreadLocalRandom random;
    private final List<Weight> weights;
    private final List<Double> intervals;
    private final double sumOfWeights;
    private final Weight maxWeight;

    public static void main(String[] args) throws URISyntaxException {
        URI server1Uri = new URI("0.0.0.1");
        URI server2Uri = new URI("0.0.0.2");
        URI server3Uri = new URI("0.0.0.3");
        URI server4Uri = new URI("0.0.0.4");
        URI server5Uri = new URI("0.0.0.5");
        URI server6Uri = new URI("0.0.0.6");
        URI server7Uri = new URI("0.0.0.7");
        URI server8Uri = new URI("0.0.0.8");
        URI server9Uri = new URI("0.0.0.9");
        URI server10Uri = new URI("0.0.0.10");
        List<ServerDetails> serverDetails = asList(
                new ServerDetails(new Weight(0.010), server1Uri)
                , new ServerDetails(new Weight(0.010), server2Uri)
                , new ServerDetails(new Weight(0.010), server3Uri)
                , new ServerDetails(new Weight(0.010), server4Uri)
                , new ServerDetails(new Weight(0.010), server5Uri)
                , new ServerDetails(new Weight(0.010), server6Uri)
                , new ServerDetails(new Weight(0.010), server7Uri)
                , new ServerDetails(new Weight(0.010), server8Uri)
                , new ServerDetails(new Weight(0.010), server9Uri)
                , new ServerDetails(new Weight(0.010), server10Uri)
        );

        int commonCount = 0;
        int server1Count = 0;
        int server2Count = 0;
        int server3Count = 0;
        int server4Count = 0;
        int server5Count = 0;
        int server6Count = 0;
        int server7Count = 0;
        int server8Count = 0;
        int server9Count = 0;
        int server10Count = 0;

        WeightedRoundRobinTreeDoubleInterval weightedRoundRobinDoubleInterval = new WeightedRoundRobinTreeDoubleInterval(serverDetails);

        //тест
        long start = System.nanoTime();

        for (; commonCount < 1_000_000; commonCount++) {
            ServerDetails nextServer = weightedRoundRobinDoubleInterval.getNextServer();
            if (nextServer.address.equals(server1Uri))
                server1Count++;
            if (nextServer.address.equals(server2Uri))
                server2Count++;
            if (nextServer.address.equals(server3Uri))
                server3Count++;
            if (nextServer.address.equals(server4Uri))
                server4Count++;
            if (nextServer.address.equals(server5Uri))
                server5Count++;
            if (nextServer.address.equals(server6Uri))
                server6Count++;
            if (nextServer.address.equals(server7Uri))
                server7Count++;
            if (nextServer.address.equals(server8Uri))
                server8Count++;
            if (nextServer.address.equals(server9Uri))
                server9Count++;
            if (nextServer.address.equals(server10Uri))
                server10Count++;
        }
        System.out.println("All " + commonCount
                + "\n server1 " + (double) server1Count / (double) commonCount
                + "\n server2 " + (double) server2Count / (double) commonCount
                + "\n server3 " + (double) server3Count / (double) commonCount
                + "\n server4 " + (double) server4Count / (double) commonCount
                + "\n server5 " + (double) server5Count / (double) commonCount
                + "\n server6 " + (double) server6Count / (double) commonCount
                + "\n server7 " + (double) server7Count / (double) commonCount
                + "\n server8 " + (double) server8Count / (double) commonCount
                + "\n server9 " + (double) server9Count / (double) commonCount
                + "\n server10 " + (double) server10Count / (double) commonCount
        );

        long finish = System.nanoTime();
        long timeConsumedMillis = finish - start;
        System.out.println("Время выполнения" + timeConsumedMillis);
    }

    public ServerDetails getNextServer() {
        //1.Рендомное число (равновероятное) где верхнее значение округлено до суммы весов(например 16)
        double randNumb = random.nextDouble((sumOfWeights));
        return getNearWeight(randNumb);
    }

    /**
     * равновероятно извлекает объект из списка
     * @param serverDetailsList список с повторяющимися значениями
     * @return любое из значений
     */
    private ServerDetails equalExtractValue(List<ServerDetails> serverDetailsList) {
        if (serverDetailsList.size() == 1) {
            return serverDetailsList.get(0);
        }
        return serverDetailsList.get(this.random.nextInt(serverDetailsList.size()));
    }

    public URI getNextServerUri() {
        return getNextServer().address;
    }

    public WeightedRoundRobinTreeDoubleInterval(List<ServerDetails> serverDetailsList) {
        serverDetailsList.sort(Comparator.comparing(o -> o.weight));
        random = ThreadLocalRandom.current(); //для генерации числа в нужно диапазоне
        weights = initWeights(serverDetailsList);
        maxWeight = weights.get(weights.size() - 1);
        sumOfWeights = computeSumOfWeights(weights);
        intervals = computeIntervals(weights);
        serversByWeightNavi = initIntervalTree(intervals,serverDetailsList);
    }

    private ServerDetails getNearWeight(double random) {
        return equalExtractValue(serversByWeightNavi.ceilingEntry(random).getValue());
    }

    private double computeSumOfWeights(List<Weight> weights) {
        double sum = 0.0;
        for (Weight weight : weights) {
            sum += weight.value;
        }
        return sum;
    }

    /**
     * инициализирует дерево где key - weight(double)
     * @param intervals список интервалов вычисленных для serverDetailsList
     * @param serverDetailsList список serverDetails
     * @return дерево упорядоченное по весам
     */
    private NavigableMap<Double,List<ServerDetails>> initIntervalTree(List<Double> intervals, List<ServerDetails> serverDetailsList){

        NavigableMap<Double,List<ServerDetails>> serversByWeightNavi = new TreeMap<>();
        for (int i = 0; i < serverDetailsList.size(); i++) {
            int finalI = i;
            serversByWeightNavi.computeIfPresent(intervals.get(i), (weight, sdList) -> { // если значение есть добавиь в список
                sdList.add(serverDetailsList.get(finalI));
                return sdList;
            });
            serversByWeightNavi.computeIfAbsent(intervals.get(i), weight -> { // если значения нет создать список
                ArrayList<ServerDetails> sdList = new ArrayList<>();
                sdList.add(serverDetailsList.get(finalI));
                return sdList;
            });
        }
        return serversByWeightNavi;
    }

    /**
     * вычисляет массив который
     * хранит верхнюю границу интервала для каждого веса в массиве аргумента weigths
     * @param weights веса упорядоченные от min to max
     * @return массив интервалов
     */
    private List<Double> computeIntervals(List<Weight> weights){
        ArrayList<Double> intervals = new ArrayList<>();
        double sum = 0;
        for (Weight weight: weights){
            sum += weight.value;
            intervals.add(sum);
        }
        return intervals;
    }

    private List<Weight> initWeights(List<ServerDetails> serverDetailsList) {
        List<Weight> weights = serverDetailsList.stream()
                .map(serverDetails -> serverDetails.weight)
                .collect(Collectors.toList());
        weights.sort(Weight::compareTo);
        return weights;
    }

    private void checkRandomArg(double random) {
        if (random > sumOfWeights && random < 0)
            throw new IllegalArgumentException("random number must be less " +
                    "them sum Of All weights and More then 0");
    }




}





