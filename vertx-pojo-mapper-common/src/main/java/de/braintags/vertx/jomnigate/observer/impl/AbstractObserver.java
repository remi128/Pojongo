/*
 * #%L
 * vertx-pojo-mapper-common
 * %%
 * Copyright (C) 2017 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.vertx.jomnigate.observer.impl;

import java.util.Properties;

import de.braintags.vertx.jomnigate.observer.IObserver;

/**
 * An abstract implementation which contains the properties
 * 
 * @author Michael Remme
 * 
 */
public abstract class AbstractObserver implements IObserver {
  private Properties properties = new Properties();

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.vertx.jomnigate.observer.IObserver#getObserverProperties()
   */
  @Override
  public Properties getObserverProperties() {
    return properties;
  }

}
