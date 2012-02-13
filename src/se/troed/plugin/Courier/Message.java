package se.troed.plugin.Courier;

import com.avaje.ebean.validation.NotEmpty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class Message implements Serializable {
    @Id
    private Integer id; // int so that we don't have to rebuild later, make sure to cast to short for now

    @ManyToOne
//    @NotNull
    private Receiver receiver;

    @NotEmpty
    @Column(length = 32)
    private String sender;

    // Q: What is the maximum size of a VARCHAR in SQLite?
    // A: SQLite does not enforce the length of a VARCHAR. You can declare a VARCHAR(10) and SQLite will be
    // happy to let you put 500 characters in it. And it will keep all 500 characters intact - it never truncates.
    @Column(columnDefinition = "LONGTEXT")
    private String message;

    private int mdate;
    private boolean delivered;
    private boolean read;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMdate() {
        return mdate;
    }

    public void setMdate(int mdate) {
        this.mdate = mdate;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

