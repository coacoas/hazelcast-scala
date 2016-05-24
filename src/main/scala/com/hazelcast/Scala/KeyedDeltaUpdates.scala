package com.hazelcast.Scala

import java.util.Map.Entry
import concurrent.Future
import language.higherKinds

private[Scala] trait KeyedDeltaUpdates[K, V] {
  type UpdateR[T]

  def upsertAndGet(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[V]
  def updateAndGet(key: K)(updateIfPresent: V => V): UpdateR[Option[V]]
  def updateAndGetIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[V]]
  def upsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[UpsertResult]
  def update(key: K)(updateIfPresent: V => V): UpdateR[Boolean]
  def updateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Boolean]
  def getAndUpsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[Option[V]]
  def getAndUpdate(key: K)(updateIfPresent: V => V): UpdateR[Option[V]]
  def getAndUpdateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[(V, Boolean)]]

}

private[Scala] object KeyedDeltaUpdates {
  final class UpsertEP[V](val insertIfMissing: V, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, UpsertResult] {
    def onEntry(entry: Entry[Any, V]): UpsertResult =
      entry.value match {
        case null =>
          entry.value = insertIfMissing
          Insert
        case value =>
          entry.value = updateIfPresent(value)
          Update
      }
  }
  final class UpsertAndGetEP[V](val insertIfMissing: V, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, V] {
    def onEntry(entry: Entry[Any, V]): V = {
      entry.value match {
        case null =>
          entry.value = insertIfMissing
          null.asInstanceOf[V] // Return `null` because we already have value locally
        case value =>
          entry.value = updateIfPresent(value)
          entry.value
      }
    }
  }
  final class UpdateEP[V](val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, Boolean] {
    def onEntry(entry: Entry[Any, V]): Boolean = {
      entry.value match {
        case null => false
        case value =>
          entry.value = updateIfPresent(value)
          true
      }
    }
  }
  final class UpdateIfEP[V](val cond: V => Boolean, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, Boolean] {
    def onEntry(entry: Entry[Any, V]): Boolean = {
      entry.value match {
        case null => false
        case value if cond(value) =>
          entry.value = updateIfPresent(value)
          true
        case _ => false
      }
    }
  }
  final class UpdateAndGetEP[V](val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, V] {
    def onEntry(entry: Entry[Any, V]): V =
      entry.value match {
        case null => null.asInstanceOf[V]
        case value =>
          entry.value = updateIfPresent(value)
          entry.value
      }
  }
  final class UpdateAndGetIfEP[V](val cond: V => Boolean, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, V] {
    def onEntry(entry: Entry[Any, V]): V =
      entry.value match {
        case null => null.asInstanceOf[V]
        case value if cond(value) =>
          entry.value = updateIfPresent(value)
          entry.value
        case value => null.asInstanceOf[V]
      }
  }
  final class GetAndUpsertEP[V](val insertIfMissing: V, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, V] {
    def onEntry(entry: Entry[Any, V]): V = {
      entry.value match {
        case null =>
          entry.value = insertIfMissing
          null.asInstanceOf[V]
        case value =>
          entry.value = updateIfPresent(value)
          value
      }
    }
  }
  final class GetAndUpdateEP[V](val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, V] {
    def onEntry(entry: Entry[Any, V]): V =
      entry.value match {
        case null => null.asInstanceOf[V]
        case value =>
          entry.value = updateIfPresent(value)
          value
      }
  }
  final class GetAndUpdateIfEP[V](val cond: V => Boolean, val updateIfPresent: V => V)
      extends SingleEntryCallbackUpdater[Any, V, (V, Boolean)] {
    def onEntry(entry: Entry[Any, V]): (V, Boolean) =
      entry.value match {
        case null => null
        case value if cond(value) =>
          entry.value = updateIfPresent(value)
          value -> true
        case value =>
          value -> false
      }
  }
}

private[Scala] trait KeyedIMapDeltaUpdates[K, V]
    extends KeyedDeltaUpdates[K, V] {
  self: HzMap[K, V] =>

  import java.util.Map.Entry
  import com.hazelcast.core.IMap

  type UpdateR[T] = T

  def upsertAndGet(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[V] =
    async.upsertAndGet(key, insertIfMissing)(updateIfPresent).await
  def updateAndGet(key: K)(updateIfPresent: V => V): UpdateR[Option[V]] =
    async.updateAndGet(key)(updateIfPresent).await
  def upsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[UpsertResult] =
    async.upsert(key, insertIfMissing)(updateIfPresent).await
  def update(key: K)(updateIfPresent: V => V): UpdateR[Boolean] =
    async.update(key)(updateIfPresent).await
  def getAndUpsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): UpdateR[Option[V]] =
    async.getAndUpsert(key, insertIfMissing)(updateIfPresent).await
  def getAndUpdate(key: K)(updateIfPresent: V => V): UpdateR[Option[V]] =
    async.getAndUpdate(key)(updateIfPresent).await
  def updateAndGetIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[V]] =
    async.updateAndGetIf(cond, key)(updateIfPresent).await
  def updateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Boolean] =
    async.updateIf(cond, key)(updateIfPresent).await
  def getAndUpdateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[(V, Boolean)]] =
    async.getAndUpdateIf(cond, key)(updateIfPresent).await

}

private[Scala] trait KeyedIMapAsyncDeltaUpdates[K, V] extends KeyedDeltaUpdates[K, V] {
  import com.hazelcast.core.IMap

  protected def imap: IMap[K, V]

  type UpdateR[T] = Future[T]

  def upsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): Future[UpsertResult] = {
    val ep = new KeyedDeltaUpdates.UpsertEP(insertIfMissing, updateIfPresent)
    val callback = ep.newCallback()
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def upsertAndGet(key: K, insertIfMissing: V)(updateIfPresent: V => V): Future[V] = {
    val ep = new KeyedDeltaUpdates.UpsertAndGetEP(insertIfMissing, updateIfPresent)
    val callback = ep.newCallback(insertIfMissing)
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def updateAndGet(key: K)(updateIfPresent: V => V): Future[Option[V]] = {
    val ep = new KeyedDeltaUpdates.UpdateAndGetEP(updateIfPresent)
    val callback = ep.newCallbackOpt
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def update(key: K)(updateIfPresent: V => V): Future[Boolean] = {
    val ep = new KeyedDeltaUpdates.UpdateEP(updateIfPresent)
    val callback = ep.newCallback()
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def getAndUpsert(key: K, insertIfMissing: V)(updateIfPresent: V => V): Future[Option[V]] = {
    val ep = new KeyedDeltaUpdates.GetAndUpsertEP(insertIfMissing, updateIfPresent)
    val callback = ep.newCallbackOpt
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def getAndUpdate(key: K)(updateIfPresent: V => V): Future[Option[V]] = {
    val ep = new KeyedDeltaUpdates.GetAndUpdateEP(updateIfPresent)
    val callback = ep.newCallbackOpt
    imap.submitToKey(key, ep, callback)
    callback.future
  }

  def updateAndGetIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[V]] = {
    val ep = new KeyedDeltaUpdates.UpdateAndGetIfEP(cond, updateIfPresent)
    val callback = ep.newCallbackOpt
    imap.submitToKey(key, ep, callback)
    callback.future
  }
  def updateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Boolean] = {
    val ep = new KeyedDeltaUpdates.UpdateIfEP(cond, updateIfPresent)
    val callback = ep.newCallback()
    imap.submitToKey(key, ep, callback)
    callback.future
  }
  def getAndUpdateIf(cond: V => Boolean, key: K)(updateIfPresent: V => V): UpdateR[Option[(V, Boolean)]] = {
    val ep = new KeyedDeltaUpdates.GetAndUpdateIfEP(cond, updateIfPresent)
    val callback = ep.newCallbackOpt
    imap.submitToKey(key, ep, callback)
    callback.future
  }

}
