package it.niedermann.nextcloud.deck.persistence.sync.adapters.db.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import it.niedermann.nextcloud.deck.model.Stack;
import it.niedermann.nextcloud.deck.model.full.FullStack;

@Dao
public interface StackDao extends GenericDao<Stack> {

    @Query("SELECT * FROM stack WHERE accountId = :accountId AND boardId = :localBoardId")
    LiveData<List<Stack>> getStacksForBoard(final long accountId, final long localBoardId);

    @Query("SELECT * FROM stack WHERE accountId = :accountId and boardId = :localBoardId and id = :remoteId")
    LiveData<Stack> getStackByRemoteId(final long accountId, final long localBoardId, final long remoteId);


    @Transaction
    @Query("SELECT * FROM stack WHERE accountId = :accountId and boardId = :localBoardId and id = :remoteId")
    FullStack getFullStackByRemoteIdDirectly(final long accountId, final long localBoardId, final long remoteId);

    @Transaction
    @Query("SELECT * FROM stack WHERE accountId = :accountId AND boardId = :localBoardId")
    LiveData<List<FullStack>> getFullStacksForBoard(final long accountId, final long localBoardId);

    @Transaction
    @Query("SELECT * FROM stack WHERE accountId = :accountId and boardId = :localBoardId and id = :remoteId")
    LiveData<FullStack> getFullStackByRemoteId(final long accountId, final long localBoardId, final long remoteId);

}