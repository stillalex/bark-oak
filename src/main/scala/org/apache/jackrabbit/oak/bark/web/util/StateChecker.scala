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

import org.apache.wicket.{ Component, Page }
import org.apache.wicket.util.visit.{ IVisit, IVisitor }

object StateChecker {

  //
  // add the following code on a page
  //
  //  override def onBeforeRender() {
  //    super.onBeforeRender();
  //    StateChecker.checkIfPageStateless(this);
  //  }

  def checkIfPageStateless(p: Page) {
    println("StateCheker check");
    if (p.isPageStateless()) {
      println("StateChecker check - page is good");
      return ;
    }

    p.visitChildren(classOf[Component], new IVisitor[Component, Void]() {
      def component(component: Component, visit: IVisit[Void]) = {
        if (!component.isStateless()) {
          println("StateChecker check - component " + component.getMarkupId() + " is NOT stateless!! ");
        } else {
          println("StateChecker check - component " + component.getMarkupId() + " is stateless");
        }
      }
    });
  }
}