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

import org.apache.jackrabbit.mk.core.MicroKernelImpl
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.api.{ ContentRepository, ContentSession, Root }
import org.apache.jackrabbit.oak.plugins.commit.{ ConflictValidatorProvider, JcrConflictHandler }
import org.apache.jackrabbit.oak.plugins.index.lucene.{ LuceneIndexEditorProvider, LuceneIndexProvider }
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneInitializerHelper
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider
import org.apache.jackrabbit.oak.plugins.index.property.{ PropertyIndexEditorProvider, PropertyIndexProvider }
import org.apache.jackrabbit.oak.plugins.name.{ NameValidatorProvider, NamespaceValidatorProvider }
import org.apache.jackrabbit.oak.plugins.nodetype.{ RegistrationEditorProvider, TypeEditorProvider }
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.plugins.version.VersionEditorProvider
import org.apache.jackrabbit.oak.security.SecurityProviderImpl
import org.apache.jackrabbit.oak.spi.commit.EditorHook
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters
import org.apache.jackrabbit.oak.spi.security.authentication.ConfigurationUtil
import javax.jcr.{ GuestCredentials, SimpleCredentials }
import javax.security.auth.login.Configuration
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneIndexHelper
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore
import org.apache.jackrabbit.oak.plugins.segment.file.FileStore

trait BarkOakHelper {

  var repository: Option[ContentRepository] = None;

  var session: Option[ContentSession] = None;

  var root: Option[Root] = None;

  def initOak() = {
    Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));
    repository = Some(createRepository)
    session = guestSession(repository);
    root = latestRoot(session);
  }

  def isRO(): Boolean = isReadOnly(session);

  def login(u: String, p: String): Either[String, ContentSession] = login(repository.get, u, p) match {
    case Right(u) ⇒ {
      session = Some(u);
      root = latestRoot(session);
      return Right(u);
    }
    case Left(u) ⇒ {
      return Left(u);
    }
  }

  def logout() = if (!isReadOnly(session)) {
    session = guestSession(repository);
    root = latestRoot(session);
  }

  // ----------------------------------------------------
  // OAK REPOSITORY
  // ----------------------------------------------------

  private[bark] def createRepository(): ContentRepository =
    new Oak(new SegmentNodeStore(new FileStore("bark-oak")))
      .`with`(new InitialContent())

      .`with`(JcrConflictHandler.JCR_CONFLICT_HANDLER)
      .`with`(new EditorHook(new VersionEditorProvider()))

      .`with`(new SecurityProviderImpl(buildSecurityConfig()))

      .`with`(new NameValidatorProvider())
      .`with`(new NamespaceValidatorProvider())
      .`with`(new TypeEditorProvider())
      .`with`(new RegistrationEditorProvider())
      .`with`(new ConflictValidatorProvider())

      .`with`(new PropertyIndexEditorProvider())

      .`with`(new PropertyIndexProvider())
      .`with`(new NodeTypeIndexProvider())

      .`with`(new LuceneInitializerHelper("luceneGlobal", LuceneIndexHelper.JR_PROPERTY_INCLUDES).async())
      .`with`(new LuceneIndexEditorProvider())
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

  // ----------------------------------------------------
  // OAK Security Setup?
  // ----------------------------------------------------

  import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
  import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
  import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
  import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
  import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
  import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
  import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
  import scala.collection.JavaConversions._

  private[bark] def buildSecurityConfig(): ConfigurationParameters = {
    val userConfig: Map[String, Object] = Map(
      UserConstants.PARAM_GROUP_PATH -> "/home/groups",
      UserConstants.PARAM_USER_PATH -> "/home/users",
      UserConstants.PARAM_DEFAULT_DEPTH -> new Integer(1),
      AccessControlAction.USER_PRIVILEGE_NAMES -> Array(PrivilegeConstants.JCR_ALL),
      AccessControlAction.GROUP_PRIVILEGE_NAMES -> Array(PrivilegeConstants.JCR_READ),
      ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR -> ImportBehavior.NAME_BESTEFFORT);

    return new ConfigurationParameters(Map(
      UserConfiguration.NAME -> new ConfigurationParameters(userConfig)));
  }

}