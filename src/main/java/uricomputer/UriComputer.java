package uricomputer;


import java.net.URI;

import static java.util.Objects.hash;

public interface UriComputer {

    URI getNextServerUri();
    ServerDetails getNextServer();

    class ServerDetails {
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

    class Weight implements Comparable<Weight> {
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
}
