package it.niedermann.nextcloud.deck.model.full;

import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

import it.niedermann.nextcloud.deck.model.AccessControl;
import it.niedermann.nextcloud.deck.model.Board;
import it.niedermann.nextcloud.deck.model.Label;
import it.niedermann.nextcloud.deck.model.User;
import it.niedermann.nextcloud.deck.model.interfaces.IRemoteEntity;

public class FullBoard implements IRemoteEntity {
    @Embedded
    public Board board;

    @Relation(entity = Label.class, parentColumn = "localId", entityColumn = "localId")
    public List<Label> labels;

    @Relation(parentColumn = "ownerId", entityColumn = "localId")
    public List<User> owner;

    @Relation(entity = AccessControl.class, parentColumn = "localId", entityColumn = "boardId")
    public List<AccessControl> participants;


    public List<User> getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        List<User> user = new ArrayList<>();
        user.add(owner);
        this.owner = user;
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
    }

    @Ignore
    @Override
    public IRemoteEntity getEntity() {
        return board;
    }

    public void setOwner(List<User> owner) {
        this.owner = owner;
    }

    public List<AccessControl> getParticipants() {
        return participants;
    }

    public void setParticipants(List<AccessControl> participants) {
        this.participants = participants;
    }

    @Override
    public String toString() {
        return "FullBoard{" +
                "board=" + board +
                ", labels=" + labels +
                ", owner=" + owner +
                ", participants=" + participants +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FullBoard fullBoard = (FullBoard) o;

        if (board != null ? !board.equals(fullBoard.board) : fullBoard.board != null) return false;
        if (labels != null ? !labels.equals(fullBoard.labels) : fullBoard.labels != null)
            return false;
        if (owner != null ? !owner.equals(fullBoard.owner) : fullBoard.owner != null) return false;
        return participants != null ? participants.equals(fullBoard.participants) : fullBoard.participants == null;
    }

    @Override
    public int hashCode() {
        int result = board != null ? board.hashCode() : 0;
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (participants != null ? participants.hashCode() : 0);
        return result;
    }
}
