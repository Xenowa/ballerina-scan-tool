/*
 * SonarScanner CLI
 * Copyright (C) 2011-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.wso2.ballerina.platforms.sonarqubeold;

import java.util.Properties;

class Cli {

  private boolean debugEnabled = false;
  private boolean displayVersionOnly = false;
  private final Properties props = new Properties();
  private final Logs logger;

  public Cli(Logs logger) {
    this.logger = logger;
  }

  Properties properties() {
    return props;
  }

  Cli parse() {
    reset();
    props.putAll(System.getProperties());
    return this;
  }

  private void reset() {
    props.clear();
    debugEnabled = false;
    displayVersionOnly = false;
  }
}
