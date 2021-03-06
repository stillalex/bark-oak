BARK, THE OAK EXPLORER
=======================

Bark is written in Scala and uses the Apache Wicket Framework for web dev, Bootstrap & font awesome for UI and (obviously) Apache Jackrabbit Oak for the backend.

Components
----------

 * Apache Jackrabbit Oak (1.6.1): http://jackrabbit.apache.org/oak
 * Apache Wicket (7.7.0): http://wicket.apache.org/
 * Bootstrap (2.2.2): http://twitter.github.com/bootstrap
 * Font Awesome (v3.0): http://fortawesome.github.com/Font-Awesome
 * Built using the Scala programming language (2.12.2): http://www.scala-lang.org

How to start
------------
  mvn clean jetty:run

and you are good to go

or if you already have a repository

  mvn -Dbark.home=/path/to/the/segmentstore jetty:run

License
-------

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
