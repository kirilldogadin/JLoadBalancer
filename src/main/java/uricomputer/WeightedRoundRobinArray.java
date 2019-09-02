package uricomputer;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Вычисляет uri согласно приориетам весов
 * точность округлена до precisionLimit из-за реализации на основе массива
 */
public class WeightedRoundRobinArray implements UriComputer {
    private final ThreadLocalRandom random;
    private final List<Weight> weights;
    private final double sumOfWeights;
    private final Weight maxWeight;
    private final List<ServerDetails> serverDetailsList;
    private final int weightСoefficient;
    private final int precisionLimit;

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
                new ServerDetails(new Weight(10000500.000), server1Uri)
                , new ServerDetails(new Weight(10000000.000), server2Uri)
                , new ServerDetails(new Weight(10000500.000), server3Uri)
                , new ServerDetails(new Weight(10000000.000), server4Uri)
                , new ServerDetails(new Weight(10500000.000), server5Uri)
                , new ServerDetails(new Weight(10000000.000), server6Uri)
                , new ServerDetails(new Weight(10000000.000), server7Uri)
                , new ServerDetails(new Weight(10000000.000), server8Uri)
                , new ServerDetails(new Weight(10000000.000), server9Uri)
                , new ServerDetails(new Weight(10005000.000), server10Uri)
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

        WeightedRoundRobinArray weightedRoundRobin = new WeightedRoundRobinArray(serverDetails, 5);

        //тест
        long start = System.nanoTime();
// поиск смысла жизни ...
        for (; commonCount < 1_000_000; commonCount++) {
            ServerDetails nextServer = weightedRoundRobin.getNextServer();
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
        int randNumb = random.nextInt(((serverDetailsList.size())));
        return serverDetailsList.get(randNumb);
    }

    public URI getNextServerUri() {
        return getNextServer().address;
    }

    public WeightedRoundRobinArray(List<ServerDetails> serverDetailsList, int precisionLimit) {
        this.precisionLimit = precisionLimit;
        random = ThreadLocalRandom.current(); //для генерации числа в нужно диапазоне
        weights = new ArrayList<>();
        serverDetailsList = optimizeValueCount(serverDetailsList);

        serverDetailsList.forEach(serverDetails -> {
            weights.add(serverDetails.weight);
        });
        weights.sort(Weight::compareTo);
        maxWeight = weights.get(weights.size() - 1);
        sumOfWeights = computeSumOfWeights(weights);
        weightСoefficient = computeСoefficient(weights, this.precisionLimit);
        this.serverDetailsList = initList(serverDetailsList);
    }

    private double computeSumOfWeights(List<Weight> weights) {
        double sum = 0.0;
        for (Weight weight : weights) {
            sum += weight.value;
        }
        return sum;
    }

    private List<ServerDetails> initList(List<ServerDetails> serverDetailsListIn) {
        List<ServerDetails> serverDetailsOut = new ArrayList<>();
        int genGcd = 1;

        //заполняем массив
        for (ServerDetails serverDetails : serverDetailsListIn) {
            int addressValueCount = (int)(serverDetails.weight.value * weightСoefficient);
            for (int j = 0; j < addressValueCount; j++) {
                serverDetailsOut.add(serverDetails);
            }
        }
        return serverDetailsOut;
    }

    /**
     * оптмимизация кол-ва записией (общий множитель весов)
     * @param serverDetailsListIn
     * @return
     */
    private List<ServerDetails> optimizeValueCount(List<ServerDetails> serverDetailsListIn){
        List<ServerDetails> optimizedServerDetailsList = serverDetailsListIn;
        int genGcd = 1;
        for (int i = 0; i < serverDetailsListIn.size() - 2; i++) { // ищем общий множитель
            int curGcd = gcd(serverDetailsListIn.get(i).weight.value.intValue(),
                    serverDetailsListIn.get(i + 1).weight.value.intValue());
            if (curGcd > genGcd)
                genGcd = curGcd;
        }

        if (genGcd > 1){ // если есть общий множитель
            int finalGenGcd = genGcd;
            optimizedServerDetailsList = serverDetailsListIn.stream()
                    .map(serverDetails -> new ServerDetails(
                            new Weight(serverDetails.weight.value / finalGenGcd),
                            serverDetails.address))
                    .collect(Collectors.toList());
        }
        return optimizedServerDetailsList;
    }

    private int computeСoefficient(List<Weight> weights, int precisionLimit) {
        //найти максимальную разрядность числа,но меньшую чем precisionLimit
        int maxPrecision = 1;
        for (Weight weight: weights){
            int curPrecision = new BigDecimal(Double.valueOf(weight.value).toString()).scale();
            if (Math.pow(10, curPrecision) > Math.pow(10, maxPrecision))    {
                maxPrecision = curPrecision > precisionLimit ? precisionLimit :  curPrecision;
            }
        }
        return (int) Math.pow(10, maxPrecision);
    }

    static int gcd(int a, int b) {
        if (a == 0 || b == 0) {
            return a + b; // base case
        }
        return gcd(b, a % b);
    }

    private void checkRandomArg(int random) {
        if (random > sumOfWeights && random < 0)
            throw new IllegalArgumentException("random number must be less " +
                    "them sum Of All weights and More then 0");
    }


}
