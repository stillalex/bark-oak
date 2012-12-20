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
package org.apache.jackrabbit.oak.bark.web

import org.apache.jackrabbit.oak.api.{ ContentRepository, ContentSession }
import org.apache.wicket.markup.html.WebPage
import org.apache.wicket.request.mapper.parameter.PageParameters
import org.apache.jackrabbit.oak.bark.BarkApp
import org.apache.jackrabbit.oak.api.Root

abstract class BasePage(pp: PageParameters) extends WebPage(pp) {

  def this() = this(null);

  def getA(): BarkApp = getApplication().asInstanceOf[BarkApp];

  def oakRepository(): ContentRepository = getA.repository.get

  def oakSession(): ContentSession = getA.session.get

  def oakRoot(): Option[Root] = getA.root

}