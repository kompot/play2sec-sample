/*
 * Copyright (c) 2013
 */

package model

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONDocumentReader
import reactivemongo.bson.BSONDocumentWriter
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.commands.Count
import reactivemongo.core.commands.GetLastError
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.ReactiveMongoPlugin
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

trait MongoService[A <: MongoEntity, B <: BSONDocumentReader[A], C <: BSONDocumentWriter[A]] {
  protected def db =  ReactiveMongoPlugin.db
  protected def collection = db[BSONCollection](collectionName)
  protected def collectionName: String
  protected def awaitJournalCommit: GetLastError = GetLastError(awaitJournalCommit = true)
  implicit protected def bsonDocumentReader: B
  implicit protected def bsonDocumentWriter: C

  def insert(a: A): Future[LastError] = collection.insert(a, awaitJournalCommit)
  def update(a: A): Future[LastError] = collection.update(BSONDocument("_id" -> a._id), a, awaitJournalCommit, upsert = false)

  def get(id: BSONObjectID): Future[Option[A]] = collection.find(BSONDocument("_id" -> id)).one[A]

  def get(id: Option[BSONObjectID]): Future[Option[A]] = {
    id match {
      case Some(i) => get(i)
      case None    => Future { None }
    }
  }

  def exists(id: BSONObjectID): Future[Boolean] = get(id).map(_.isDefined)

  def getByIds(ids: Array[BSONObjectID]): Future[List[A]] = {
    collection.find(BSONDocument("_id" -> BSONDocument("$in" -> ids))).cursor[A].collect[List](1000, stopOnError = false)
  }

  def query(doc: BSONDocument = BSONDocument()): Future[List[A]] =
    collection.find(doc).cursor[A].collect[List](1000, stopOnError = false)

  def queryOne(doc: BSONDocument = BSONDocument()): Future[Option[A]] = collection.find(doc).one[A]

  def remove(id: BSONObjectID) = collection.remove(BSONDocument("_id" -> id))

  def countAll() = db.command(Count(collectionName, None))
  def removeAll() = collection.remove(BSONDocument())
}

object MongoWait {
  val defaultTimeout = 10.second

  /**
   * Use synchronous data retrieval only when absolutely required.
   * E. g. to check web forms for errors. Better find a way to avoid using this.
   */
  @deprecated("Make use of Scala futures.", "0.0.5")
  def apply[T](future: Future[T]): T = Await.result(future, defaultTimeout)
}
