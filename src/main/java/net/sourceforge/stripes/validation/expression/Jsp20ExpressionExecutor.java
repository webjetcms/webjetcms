/* Copyright 2008 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.stripes.validation.expression;

import jakarta.el.ExpressionFactory;
import net.sourceforge.stripes.util.Log;

/**
 * An implementation of {@link ExpressionExecutor} that uses the container's built in JSP2.0 EL
 * implementation. This requires that the DispatcherServlet allocates a {@link
 * jakarta.servlet.jsp.PageContext} object earlier in the request cycle in order to gain access to
 * the ExpressionEvaluator. This can cause problems in some containers.
 *
 * @author Tim Fennell
 * @since Stripes 1.5
 */
public class Jsp20ExpressionExecutor extends ExpressionExecutorSupport {
  private static final Log log = Log.getInstance(Jsp20ExpressionExecutor.class);

  /**
   * Returns the Jakarta ExpressionFactory implementation available on classpath.
   *
   * @return an ExpressionFactory if possible, or null otherwise
   */
  @Override
  protected ExpressionFactory getExpressionFactory() {
    try {
      return ExpressionFactory.newInstance();
    } catch (Exception e) {
      log.error("Could not initialize Jakarta ExpressionFactory. Expression validation is disabled.", e);
      return null;
    }
  }
}
