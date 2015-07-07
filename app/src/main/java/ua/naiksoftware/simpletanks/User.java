package ua.naiksoftware.simpletanks;

/**
 * Created by Naik on 07.07.15.
 */
public class User {

    private String name;
    private String ip;
    private long id;

    public User(long id) {
        this.id = id;
    }

    public User(String name, long id, String ip) {
        this.name = name;
        this.id = id;
        this.ip = ip;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof User) {
            return ((User) o).getId() == id;
        } else {
            return false;
        }
    }
}
