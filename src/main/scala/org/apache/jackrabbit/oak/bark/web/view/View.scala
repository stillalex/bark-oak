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
package org.apache.jackrabbit.oak.bark.web.view

import scala.collection.JavaConversions.{ asScalaBuffer, iterableAsScalaIterable, seqAsJavaList }

import org.apache.jackrabbit.oak.api.{ PropertyState, Tree }
import org.apache.jackrabbit.oak.bark.web.BaseTemplatePage
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.wicket.Component
import org.apache.wicket.markup.html.WebMarkupContainer
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.markup.html.form.{ Button, RequiredTextField, StatelessForm }
import org.apache.wicket.markup.html.link.BookmarkablePageLink
import org.apache.wicket.markup.html.panel.FeedbackPanel
import org.apache.wicket.markup.repeater.Item
import org.apache.wicket.markup.repeater.data.{ DataView, ListDataProvider }
import org.apache.wicket.model.{ LoadableDetachableModel, PropertyModel }
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException
import org.apache.wicket.request.mapper.parameter.PageParameters

class View(pp: PageParameters) extends BaseTemplatePage(pp) {

  val path: String = pp.get("p").toString("/");

  val root: LoadableDetachableModel[Tree] = new LoadableDetachableModel[Tree]() {
    def load(): Tree = {
      val r = oakSession.getLatestRoot().getTree(path);
      if (r == null) {
        throw new AbortWithHttpErrorCodeException(404);
      }
      return r;
    }
  }

  //
  // --

  setStatelessHint(true);
  buildBC(root.getObject(), path);
  buildChildren(root.getObject(), path);
  buildProps(root.getObject());

  add(buildFormContainer().setVisibilityAllowed(!getA.isRO));

  //
  // --

  private[view] def buildBC(root: Tree, path: String) {
    add(new BookmarkablePageLink("root", classOf[View]));
    add(new Label("current", root.getName()));

    val c: List[String] = PathUtils.elements(path).toList.dropRight(1);

    add(new DataView[String]("paths", new ListDataProvider(c)) {

      override def populateItem(item: Item[String]) {
        val p: String = item.getModelObject();
        val link = selfBPL("segment", path.substring(0, path.indexOf(p) + p.length()));
        link.add(new Label("name", p));
        item.add(link);
      }
    });
  }

  private[view] def buildChildren(root: Tree, path: String) {
    val c: List[String] = root.getChildren().map(x ⇒ x.getName()).toList;
    add(new DataView[String]("children", new ListDataProvider(c)) {

      override def populateItem(item: Item[String]) {
        val p: String = item.getModelObject();
        val link = selfBPL("child", PathUtils.concat(path, p));
        link.add(new Label("name", p));
        item.add(link);
      }
    });
  }

  private[view] def selfBPL(id: String, path: String): BookmarkablePageLink[View] = {
    val pp: PageParameters = new PageParameters();
    if (!"/".equals(path)) {
      pp.set("p", path);
    }
    return new BookmarkablePageLink(id, classOf[View], pp);
  }

  private[view] def buildProps(root: Tree) {
    val p: List[(String, String)] = root.getProperties().map(x ⇒ (x.getName(), psAsString(x))).toList;

    add(new DataView[(String, String)]("properties", new ListDataProvider(p)) {

      override def populateItem(item: Item[(String, String)]) {
        val p: (String, String) = item.getModelObject();
        item.add(new Label("name", p._1));
        item.add(new Label("value", p._2));
      }
    });
  }

  private[view] def psAsString(ps: PropertyState): String = {
    if (ps.isArray()) {
      return "[" + ps.getValues().foldLeft("")((s, v) ⇒ v.getString() + ", " + s) + "]";
    }
    return ps.getValue().getString();
  }

  //
  //--
  //
  var addName: String = "";

  private[view] def buildFormContainer(): Component = {
    val con = new WebMarkupContainer("addFormContainer");
    con.add(buildForm);
    con.add(new FeedbackPanel("feedback"));
    return con;
  }

  private[view] def buildForm(): Component = {
    val form = new StatelessForm[Void]("addForm");
    form.setOutputMarkupId(true);

    val a = new RequiredTextField[String]("add", new PropertyModel[String](
      this, "addName"))

    val submit = new Button("submit") {
      override def onSubmit() =
        try {
          val root = oakSession.getLatestRoot();
          root.getTree(path).addChild(a.getModelObject);
          root.commit();

          val pp: PageParameters = new PageParameters();
          if (!"/".equals(path)) {
            pp.set("p", path);
          }
          setResponsePage(classOf[View], pp);

        } catch {
          case e: Exception ⇒ {
            e.printStackTrace()
            error(e.getMessage());
          }
        }
    };

    form.add(a);
    form.add(submit);
    form.setDefaultButton(submit);
    return form;
  }

}