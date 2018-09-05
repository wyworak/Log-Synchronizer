package wsdworak.entities;

import java.util.Arrays;
import java.util.Objects;

public class PlayerStartVideo {

    private int id;
    private String name;
    private double[] startGame;

    public PlayerStartVideo() {
    }

    public PlayerStartVideo(final int id, final String name, final double[] startGame) {
        this.id = id;
        this.name = name;
        this.startGame = startGame;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double[] getStartGame() {
        return startGame;
    }

    public void setStartGame(double[] startGame) {
        this.startGame = startGame;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerStartVideo that = (PlayerStartVideo) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Arrays.equals(startGame, that.startGame);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(id, name);
        result = 31 * result + Arrays.hashCode(startGame);
        return result;
    }

    @Override
    public String toString() {
        return "PlayerStartVideo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", startGame=" + Arrays.toString(startGame) +
                '}';
    }


    public static final class PlayerStartVideoBuilder {
        private int id;
        private String name;
        private double[] startGame;

        private PlayerStartVideoBuilder() {
        }

        public static PlayerStartVideoBuilder playerStartVideoBuilder() {
            return new PlayerStartVideoBuilder();
        }

        public PlayerStartVideoBuilder id(int id) {
            this.id = id;
            return this;
        }

        public PlayerStartVideoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PlayerStartVideoBuilder startGame(double[] startGame) {
            this.startGame = startGame;
            return this;
        }

        public PlayerStartVideo build() {
            PlayerStartVideo playerStartVideo = new PlayerStartVideo();
            playerStartVideo.setId(id);
            playerStartVideo.setName(name);
            playerStartVideo.setStartGame(startGame);
            return playerStartVideo;
        }
    }
}
