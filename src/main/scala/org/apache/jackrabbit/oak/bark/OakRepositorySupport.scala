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

import org.apache.jackrabbit.oak.api.ContentRepository
import org.apache.jackrabbit.oak.spi.state.NodeStore
import org.apache.jackrabbit.oak.plugins.version.VersionHook
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent
import org.apache.jackrabbit.oak.Oak
import org.apache.jackrabbit.oak.plugins.index.reference.ReferenceIndexProvider
import org.apache.jackrabbit.oak.plugins.name.NamespaceEditorProvider
import org.apache.jackrabbit.oak.plugins.name.NameValidatorProvider
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider
import org.apache.jackrabbit.oak.plugins.index.counter.NodeCounterEditorProvider
import org.apache.jackrabbit.oak.plugins.index.nodetype.NodeTypeIndexProvider
import org.apache.jackrabbit.oak.security.SecurityProviderImpl
import org.apache.jackrabbit.oak.plugins.index.reference.ReferenceEditorProvider
import org.apache.jackrabbit.oak.plugins.itemsave.ItemSaveValidatorProvider
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider
import org.apache.jackrabbit.oak.plugins.commit.ConflictValidatorProvider
import org.apache.jackrabbit.oak.plugins.index.property.OrderedPropertyIndexEditorProvider
import org.apache.jackrabbit.oak.plugins.observation.ChangeCollectorProvider
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder
import java.io.File
import org.apache.jackrabbit.oak.segment.SegmentNodeStore
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders
import org.apache.jackrabbit.oak.spi.security.authentication.ConfigurationUtil
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters
import javax.security.auth.login.Configuration

object OakRepositorySupport {

  def newNodeStore(): NodeStore = {
    val loc = System.getProperty("bark.home", "bark-oak");
    val fs = FileStoreBuilder.fileStoreBuilder(new File(loc)).build()
    SegmentNodeStoreBuilders.builder(fs).build();
  }

  def createRepository(store: NodeStore): ContentRepository = {

    Configuration.setConfiguration(ConfigurationUtil.getJackrabbit2Configuration(ConfigurationParameters.EMPTY));

    new Oak(store)

      .`with`(new InitialContent())

      .`with`(new VersionHook())
      .`with`(new SecurityProviderImpl())

      .`with`(new ItemSaveValidatorProvider())
      .`with`(new NameValidatorProvider())
      .`with`(new NamespaceEditorProvider())
      .`with`(new TypeEditorProvider())
      .`with`(new ConflictValidatorProvider())
      .`with`(new ChangeCollectorProvider())

      .`with`(new ReferenceEditorProvider())
      .`with`(new ReferenceIndexProvider())

      .`with`(new PropertyIndexEditorProvider())
      .`with`(new NodeCounterEditorProvider())

      .`with`(new PropertyIndexProvider())
      .`with`(new NodeTypeIndexProvider())

      .`with`(new OrderedPropertyIndexEditorProvider())

      .createContentRepository();
  }
}