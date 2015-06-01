package se.troed.plugin.Courier;

import java.io.IOException;

/**
 * ICourierDB defines methods for interacting with a Courier database.
 */
public interface ICourierDB {
    /**
     * Initializes and loads the DB.
     *
     * @return true if an existing DB was loaded; otherwise false.
     * @throws IOException If the database could not be opened.
     */
    boolean load() throws IOException;

    /**
     * Instructs the DB to persist any in-memory changes, optionally to a given file.
     *
     * This method's behavior is highly implementation-dependent.
     *
     * @param filename The file to which data should be persisted. May be null.
     * @return true if data was successfully persisted; otherwise false.
     */
    boolean save(String filename);

    /**
     * Retrieves the version of the database format/schema.
     *
     * 1 = v1.0.0
     *
     * @return -1 if the version is unknown/unset; otherwise the version number.
     */
    int getDatabaseVersion();

    /**
     * Sets the version of the database format/schema.
     *
     * May have no effect, depending on the implementation.
     *
     * @param version The version number.
     */
    void setDatabaseVersion(int version);

    // retrieves what we think is our specially allocated Map

    /**
     * Retrieves the map ID claimed by Courier for rendering letters.
     *
     * @return -1 if the map ID is unknown/not yet claimed; otherwise the map ID.
     */
    short getCourierMapId();

    /**
     * Sets the map ID claimed by Courier for rendering letters.
     *
     * @param mapId The map ID.
     */
    void setCourierMapId(short mapId);

    /**
     * Sets a message as ready for delivery.
     *
     * @param messageId     The message ID.
     * @param recipientName The recipient's user name.
     * @param senderName    The sender's user name.
     * @return true if the message was 'sent' successfully; otherwise false.
     */
    boolean sendMessage(int messageId, String recipientName, String senderName);

    /**
     * Stores a message's content.
     *
     * @param messageId  The message ID.
     * @param senderName The sender's user name.
     * @param message    The message content.
     * @param timestamp  The date, formatted as a Unix timestamp.
     * @return true if the message was stored successfully; otherwise false.
     */
    boolean storeMessage(int messageId, String senderName, String message, int timestamp);

    /**
     * Called when moving from a pre-version 1 DB to 1+.
     * It rewrites all player name keys to their lowercase equivalent.
     */
    void keysToLower();

    /**
     * Sets a message's date.
     * 
     * Used for legacy Letter conversion only.
     *
     * @param messageId The message ID.
     * @param timestamp The date, as a Unix timestamp.
     * @return true if the operation was successful; otherwise false.
     */
    boolean storeDate(int messageId, int timestamp);

    /**
     * Changes a message's ID.
     * 
     * Currently used for legacy letter conversion only, but it is generalized.
     *
     * @param oldId The original ID.
     * @param newId The new ID.
     */
    void changeId(int oldId, int newId);

    /**
     * Checks whether a player has new mail.
     *
     * @param recipientName The recipient's user name.
     * @return true if there is new mail; otherwise false.
     */
    boolean undeliveredMail(String recipientName);

    // runs through messageids, sets all unread messages to undelivered
    // returns false when there are no unread messages

    /**
     * Sets all unread messages to a user as undelivered.
     * Marks the user as having new mail if there were unread messages.
     *
     * @param recipientName The recipient's user name.
     * @return true if the user has new mail; otherwise false.
     */
    boolean deliverUnreadMessages(String recipientName);

    // runs through messageids, finds a message not read and returns the corresponding id
    // returns -1 on failure

    /**
     * Gets the message ID of the oldest unread message sent to a user.
     *
     * @param recipientName The recipient's user name.
     * @return -1 if an unread message could not be found; otherwise the message ID.
     */
    int unreadMessageId(String recipientName);

    // runs through messageids, finds a message not delivered and returns the corresponding id
    // returns -1 on failure

    /**
     * Gets the message ID of the oldest undelivered message sent to a user.
     * 
     * If there are no undelivered messages, the user will be marked as having no new mail.
     *
     * @param recipientName The recipient's user name.
     * @return -1 if an undelivered message could not be found; otherwise the message ID.
     */
    int undeliveredMessageId(String recipientName);

    // removes a single Letter from the database

    /**
     * Removes a message from the database.
     *
     * @param messageId The message ID.
     * @return true if the message was deleted successfully; otherwise false.
     */
    boolean deleteMessage(short messageId);

    /**
     * Checks if a message with the given ID exists in the database.
     *
     * @param messageId The message ID.
     * @return true if the message ID is valid; otherwise false.
     */
    boolean isValid(int messageId);

    // finds a specific messageid and returns associated player

    /**
     * Gets the recipient associated with the given message ID.
     *
     * @param messageId The message ID.
     * @return The recipient player's user name.
     */
    String getPlayer(int messageId);

    /**
     * Gets the sender of a given message.
     *
     * @param recipientName The user name of the recipient.
     * @param messageId     The message ID.
     * @return null if the sender can't be found; otherwise the sender's user name.
     */
    String getSender(String recipientName, int messageId);

    /**
     * Gets the contents of a given message.
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return null if the message can't be retrieved; otherwise the contents of the message.
     */
    String getMessage(String recipientName, int messageId);

    /**
     * Checks whether a given message has been delivered.
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return true if the message was delivered; otherwise false.
     */
    boolean getDelivered(String recipientName, int messageId);

    /**
     * Sets a given message as delivered, and updates the user's new mail status appropriately.
     * 
     * todo: unexpected side effect, we end up here if player1 takes a message intended for player2
     * exploit or remove logging of it?
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return true if successful; otherwise false.
     */
    boolean setDelivered(String recipientName, int messageId);

    /**
     * Gets the date/timestamp of a message.
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return The timestamp of the message if it can be retrieved; otherwise -1.
     */
    int getDate(String recipientName, int messageId);

    /**
     * Gets the unread status of a message.
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return false if the unread status is unavailable; otherwise either true/false depending on the unread status.
     */
    boolean getRead(String recipientName, int messageId);

    /**
     * Sets a message as read.
     *
     * @param recipientName The recipient's user name.
     * @param messageId     The message ID.
     * @return true if successful; otherwise false.
     */
    boolean setRead(String recipientName, int messageId);

    /**
     * Generates a unique ID for a message.
     *
     * @return A new message ID if successful; otherwise -1.
     */
    int generateUID();
}
