package uricomputer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.asList;

/**
 * Вычисляет uri согласно приориетам весов
 */
public class WeightedRoundRobinDummy implements UriComputer {
    private final Map<Weight, List<ServerDetails>> serversByWeight;
    private final ThreadLocalRandom random;
    private final List<Weight> weights;
    private final double sumOfWeights;
    private final Weight maxWeight;
    private final List<ServerDetails> serverDetailsList;

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
                new ServerDetails(new Weight(1.0), server1Uri)
                , new ServerDetails(new Weight(2.0), server2Uri)
                , new ServerDetails(new Weight(3.0), server3Uri)
                , new ServerDetails(new Weight(4.0), server4Uri)
                , new ServerDetails(new Weight(5.0), server5Uri)
                , new ServerDetails(new Weight(6.0), server6Uri)
                , new ServerDetails(new Weight(7.0), server7Uri)
                , new ServerDetails(new Weight(8.0), server8Uri)
                , new ServerDetails(new Weight(9.0), server9Uri)
                , new ServerDetails(new Weight(10.0), server10Uri)
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

        WeightedRoundRobinDummy weightedRoundRobin = new WeightedRoundRobinDummy(serverDetails);

        //тест
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
    }

    public ServerDetails getNextServer() {
        //1.Рендомное число (равновероятное) где верхнее значение округлено до суммы весов(например 16)
        int randNumb = random.nextInt(((serverDetailsList.size()-1)));
        return serverDetailsList.get(randNumb);
    }

    /**
     * если есть адреса с одинаковыми весами, то равновероятно между ними извлечь
     *
     * @param weight полученное ближайшее значене
     * @param random случайно сгенерированное число
     * @return любое из значений
     */
    private ServerDetails extractWeight(Weight weight, int random) {
        List<ServerDetails> serverDetailsList = serversByWeight.get(weight);
        if (serverDetailsList.size() == 1) {
            return serverDetailsList.get(0);
        }
        return serverDetailsList.get(this.random.nextInt(serverDetailsList.size()));
    }

    public URI getNextServerUri() {
        return getNextServer().address;
    }

    public WeightedRoundRobinDummy(List<ServerDetails> serverDetailsList) {
        random = ThreadLocalRandom.current(); //для генерации числа в нужно диапазоне
        serversByWeight = new HashMap<>();
        weights = new ArrayList<>();

        serverDetailsList.forEach(serverDetails -> {
            serversByWeight.computeIfPresent(serverDetails.weight, (weight, sdList) -> { // если значение есть добавиь в список
                sdList.add(serverDetails);
                return sdList;
            });
            serversByWeight.computeIfAbsent(serverDetails.weight, weight -> { // если значения нет создать список
                ArrayList<ServerDetails> sdList = new ArrayList<>();
                sdList.add(serverDetails);
                return sdList;
            });
            weights.add(serverDetails.weight);
        });
        weights.sort(Weight::compareTo);
        maxWeight = weights.get(weights.size() - 1);
        sumOfWeights = computeSumOfWeights(weights);
        this.serverDetailsList = initList(serverDetailsList,Double.valueOf(sumOfWeights).intValue());
    }

    private double computeSumOfWeights(List<Weight> weights) {
        double sum = 0.0;
        for (Weight weight : weights) {
            sum += weight.value;
        }
        return sum;
    }

    /**
     * Вернет вес для случайного числа
     *
     * @param random    должен быть от 0 до sum весов
     * @param weights   упорядочен от меньшего к большему!
     * @param maxWeight макс вес
     * @return вероятный вес согласно весам
     */
    private Weight nearWeight(int random, List<Weight> weights, Weight maxWeight) {
        checkRandomArg(random);

        for (Weight weight : weights) {
            if (weight.value > random) {
                return weight;
            }
        }
        return maxWeight;
    }


    private List<ServerDetails> initList(List<ServerDetails> serverDetailsListIn, int sumWeights) {
        List<ServerDetails> serverDetailsOut = new ArrayList<>();
        int genGcd = 1;
        //region оптмимизация кол-ва записией
//        for (int i = 0; i < serverDetailsListIn.size() - 2; i++) { // ищем общий множитель
//            int curGcd = gcd(serverDetailsListIn.get(i).weight.value.intValue(),
//                    serverDetailsListIn.get(i + 1).weight.value.intValue());
//            if (curGcd > genGcd)
//                genGcd = curGcd;
//        }

//        if (genGcd > 1){ // если есть общий множитель
//            serverDetailsOut = serverDetailsOut.stream()
//                    .map(serverDetails ->  new ServerDetails(
//                            new Weight(serverDetails.weight.value / genGcd),
//                            serverDetails.address))
//                    .collect(Collectors.toList());
//        }
        //endregion

        //заполняем массив
        for (int i = 0; i < serverDetailsListIn.size();i++ ) {
            int addressValueCount = serverDetailsListIn.get(i).weight.value.intValue();
            for (int j = 0; j < addressValueCount; j++) {
                serverDetailsOut.add(serverDetailsListIn.get(i));
            }
        }

    return serverDetailsOut;
    }

    static int gcd(int a, int b) {
        if (a == 0 || b == 0) return a + b; // base case
        return gcd(b, a % b);
    }

    private void checkRandomArg(int random) {
        if (random > sumOfWeights && random < 0)
            throw new IllegalArgumentException("random number must be less " +
                    "them sum Of All weights and More then 0");
    }


}