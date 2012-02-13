package se.troed.plugin.Courier;

import com.avaje.ebean.validation.NotEmpty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Receiver implements Serializable {

    @Id
    @NotEmpty
    @Column(length = 32)
    private String name;

    private boolean newmail;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<Message>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNewmail() {
        return newmail;
    }

    public void setNewmail(boolean newmail) {
        this.newmail = newmail;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}

