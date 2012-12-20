/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.bark

import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ ContentRepository, ContentSession, Root }
import org.apache.jackrabbit.oak.plugins.commit.{ AnnotatingConflictHandler, ConflictValidatorProvider }
import org.apache.jackrabbit.oak.plugins.index.lucene.{ LuceneIndexHookProvider, LuceneIndexProvider }
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneInitializerHelper
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider
import org.apache.jackrabbit.oak.plugins.index.property.{ PropertyIndexHookProvider, PropertyIndexProvider }
import org.apache.jackrabbit.oak.plugins.name.{ NameValidatorProvider, NamespaceValidatorProvider }
import org.apache.jackrabbit.oak.plugins.nodetype.{ DefaultTypeEditor, InitialContent, RegistrationValidatorProvider, TypeValidatorProvider }
import org.apache.jackrabbit.oak.security.{ OakConfiguration, SecurityProviderImpl }
import javax.jcr.{ GuestCredentials, SimpleCredentials }
import javax.security.auth.login.Configuration
import org.apache.jackrabbit.oak.plugins.index.p2.Property2IndexHookProvider
import org.apache.jackrabbit.oak.plugins.index.p2.Property2IndexProvider
import org.apache.jackrabbit.mk.core.MicroKernelImpl
import java.util.concurrent.Executors
import org.apache.jackrabbit.oak.plugins.version.VersionHook

trait BarkOakHelper {

  var repository: Option[ContentRepository] = None;

  var session: Option[ContentSession] = None;

  var root: Option[Root] = None;

  def initOak() = {
    Configuration.setConfiguration(new OakConfiguration());
    repository = Some(createRepository)
    session = guestSession(repository);
    root = latestRoot(session);
  }

  def isRO(): Boolean = isReadOnly(session);

  def login(u: String, p: String): Either[String, ContentSession] = login(repository.get, u, p) match {
    case Right(u) ⇒ {
      session = Some(u);
      return Right(u);
    }
    case Left(u) ⇒ {
      return Left(u);
    }
  }

  def logout() = if (!isReadOnly(session)) {
    session = guestSession(repository);
  }

  // ----------------------------------------------------
  // OAK REPOSITORY
  // ----------------------------------------------------

  private[bark] def createRepository(): ContentRepository =
    new Oak(new MicroKernelImpl())
      .`with`(new InitialContent())

      .`with`(new DefaultTypeEditor())
      .`with`(new VersionHook())

      .`with`(new SecurityProviderImpl())

      .`with`(new NameValidatorProvider())
      .`with`(new NamespaceValidatorProvider())
      .`with`(new TypeValidatorProvider())
      .`with`(new RegistrationValidatorProvider())
      .`with`(new ConflictValidatorProvider())

      .`with`(new Property2IndexHookProvider())
      .`with`(new AnnotatingConflictHandler())

      .`with`(new Property2IndexProvider())
      .`with`(new NodeTypeIndexProvider())

      .`with`(new LuceneInitializerHelper("/oak:index/luceneGlobal"))
      .`with`(new LuceneIndexHookProvider())
      .`with`(new LuceneIndexProvider())
      .createContentRepository();

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  private[bark] def guestSession(repository: Option[ContentRepository]): Option[ContentSession] =
    repository match {
      case Some(r) ⇒ Some(r.login(new GuestCredentials(), null));
      case None ⇒ None;
    }

  private[bark] def login(repo: ContentRepository, u: String, p: String): Either[String, ContentSession] =
    try {
      return Right(repo.login(new SimpleCredentials(u, p.toCharArray()), null));
    } catch {
      case e: Exception ⇒ {
        e.printStackTrace()
        Left(e.getMessage());
      }
    }

  private[bark] def isReadOnly(session: Option[ContentSession]): Boolean =
    session match {
      case Some(s) if (s.getAuthInfo().getUserID() != null && !"anonymous".equals(s.getAuthInfo().getUserID())) ⇒ return false;
      case _ ⇒ true;
    }

  private[bark] def latestRoot(sessio: Option[ContentSession]): Option[Root] =
    session match {
      case Some(s) ⇒ Some(s.getLatestRoot());
      case None ⇒ None;
    }

}