import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.asList;
import static java.util.Objects.hash;

/**
 * Вычисляет uri согласно приориетам весов
 */
public class WeightedRoundRobin {
    private final Map<Weight, List<ServerDetails>> serversByWeight;
    private final ThreadLocalRandom random;
    private final List<Weight> weights;
    private final double sumOfWeights;
    private final Weight maxWeight;

    public static void main(String[] args) throws URISyntaxException {
        URI server1Uri = new URI("0.0.0.1");
        URI server2Uri = new URI("0.0.0.2");
        URI server3Uri = new URI("0.0.0.3");
        List<ServerDetails> serverDetails = asList(
                new ServerDetails(new Weight(300.0), server1Uri)
                , new ServerDetails(new Weight(50.0), server2Uri)
                , new ServerDetails(new Weight(24.0), server3Uri)
        );

        int commonCount = 0;
        int server1Count = 0;
        int server2Count = 0;
        int server3Count = 0;

        WeightedRoundRobin weightedRoundRobin = new WeightedRoundRobin(serverDetails);

        //тест
        for (; commonCount < 1_000_000; commonCount++) {
            ServerDetails nextServer = weightedRoundRobin.getNextServer();
            if (nextServer.address.equals(server1Uri))
                server1Count++;
            if (nextServer.address.equals(server2Uri))
                server2Count++;
            if (nextServer.address.equals(server3Uri))
                server3Count++;
        }
        System.out.println("All " + commonCount
                + "\n server1 " + (double) server1Count / (double) commonCount
                + "\n server2 " + (double) server2Count / (double) commonCount
                + "\n server3 " + (double) server3Count / (double) commonCount
        );
    }

    public ServerDetails getNextServer() {
        //1.Рендомное число (равновероятное) где верхнее значение округлено до суммы весов(например 16)
        int randNumb = random.nextInt(Math.toIntExact(Math.round(sumOfWeights)));
        Weight nearWeight = nearWeight(randNumb, weights, maxWeight);
        return extractWeight(nearWeight, randNumb);
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

    public WeightedRoundRobin(List<ServerDetails> serverDetailsList) {
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

    private void checkRandomArg(int random) {
        if (random > sumOfWeights && random < 0)
            throw new IllegalArgumentException("random number must be less " +
                    "them sum Of All weights and More then 0");
    }

    //region own Types

    /**
     * Описание вес + адрес
     */
    public static class ServerDetails {
        final Weight weight;
        final URI address;

        public ServerDetails(Weight weight, URI address) {
            this.weight = weight;
            this.address = address;
        }

        public Weight getWeight() {
            return weight;
        }

        public URI getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServerDetails that = (ServerDetails) o;
            return weight.equals(that.weight) &&
                    address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return hash(weight, address);
        }
    }

    /**
     * Вес адреса сервера. Чем выше вес , тем выше вероятность возврата адреса, которому принадлжит вес
     */
    public static class Weight implements Comparable<Weight> {
        final Double value;

        public Weight(Double value) {
            this.value = value;
        }

        public Double getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Weight weight = (Weight) o;
            return value.equals(weight.value);
        }

        @Override
        public int hashCode() {
            return hash(value);
        }

        @Override
        public int compareTo(Weight another) {
            return value.compareTo(another.value);
        }
    }

    //endregion


}
