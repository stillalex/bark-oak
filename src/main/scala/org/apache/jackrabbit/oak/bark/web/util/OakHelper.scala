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
package org.apache.jackrabbit.oak.bark.web.util

import org.apache.jackrabbit.mk.api.MicroKernel
import org.apache.jackrabbit.mk.core.MicroKernelImpl
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ ContentRepository, ContentSession }
import org.apache.jackrabbit.oak.plugins.`type`.{ DefaultTypeEditor, InitialContent, TypeValidatorProvider }
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider
import org.apache.jackrabbit.oak.plugins.lucene.{ LuceneHook, LuceneIndexProvider, LuceneReindexHook }
import org.apache.jackrabbit.oak.plugins.name.{ NameValidatorProvider, NamespaceValidatorProvider }
import org.apache.jackrabbit.oak.plugins.unique.UniqueIndexHook
import org.apache.jackrabbit.oak.security.authorization.{ AccessControlValidatorProvider, PermissionValidatorProvider }
import org.apache.jackrabbit.oak.security.privilege.PrivilegeValidatorProvider
import org.apache.jackrabbit.oak.security.user.UserValidatorProvider
import org.apache.jackrabbit.oak.spi.commit.{ CommitHook, CompositeHook, CompositeValidatorProvider, ValidatingHook, ValidatorProvider }
import org.apache.jackrabbit.oak.spi.query.IndexUtils.DEFAULT_INDEX_HOME
import org.apache.jackrabbit.oak.spi.security.user.UserConfig

import javax.jcr.{ GuestCredentials, SimpleCredentials }

trait OakHelper {

  var repository: Option[ContentRepository] = None;

  var session: Option[ContentSession] = None;

  def initOak() = {
    repository = Some(createRepository)
    session = guestSession(repository);
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

  private[util] def createRepository(): ContentRepository =
    new Oak(setupInitialContent(new MicroKernelImpl())).`with`(buildDefaultCommitHook())
      .`with`(new LuceneIndexProvider(DEFAULT_INDEX_HOME))
      .createContentRepository();

  private[util] def buildDefaultCommitHook(): CommitHook =
    new CompositeHook(
      new DefaultTypeEditor(),
      new ValidatingHook(createDefaultValidatorProvider()),
      new UniqueIndexHook(),
      new LuceneReindexHook(DEFAULT_INDEX_HOME),
      new LuceneHook(DEFAULT_INDEX_HOME));

  private[util] def createDefaultValidatorProvider(): ValidatorProvider =
    new CompositeValidatorProvider(
      new NameValidatorProvider(),
      new NamespaceValidatorProvider(),
      new TypeValidatorProvider(),
      new ConflictValidatorProvider(),
      new PermissionValidatorProvider(),
      new AccessControlValidatorProvider(),
      // FIXME: retrieve from user context
      new UserValidatorProvider(new UserConfig("admin")),
      new PrivilegeValidatorProvider());

  private[util] def setupInitialContent(mk: MicroKernel): MicroKernel = {
    new InitialContent().available(mk);
    return mk;
  }

  // ----------------------------------------------------
  // OAK SESSION
  // ----------------------------------------------------

  private[util] def guestSession(repository: Option[ContentRepository]): Option[ContentSession] =
    repository match {
      case Some(r) ⇒ Some(r.login(new GuestCredentials(), null));
      case None ⇒ None;
    }

  private[util] def login(repo: ContentRepository, u: String, p: String): Either[String, ContentSession] =
    try {
      return Right(repo.login(new SimpleCredentials(u, p.toCharArray()), null));
    } catch {
      case e: Exception ⇒ {
        e.printStackTrace()
        Left(e.getMessage());
      }
    }

  private[util] def isReadOnly(session: Option[ContentSession]): Boolean =
    session match {
      case Some(s) if (s.getAuthInfo().getUserID() != null && !"anonymous".equals(s.getAuthInfo().getUserID())) ⇒ return false;
      case _ ⇒ true;
    }

}