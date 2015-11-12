/*
 * #%L
 * vertx-pojo-mapper-json
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.io.vertx.pojomapper.json.dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import de.braintags.io.vertx.pojomapper.annotation.lifecycle.AfterLoad;
import de.braintags.io.vertx.pojomapper.exception.MappingException;
import de.braintags.io.vertx.pojomapper.mapping.IField;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mapping.IObjectReference;
import de.braintags.io.vertx.pojomapper.mapping.IStoreObject;
import de.braintags.io.vertx.pojomapper.mapping.datastore.IColumnInfo;
import de.braintags.io.vertx.util.CounterObject;
import de.braintags.io.vertx.util.ErrorObject;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * An implementation of {@link IStoreObject}, which uses a JsonObject as internal container
 * 
 * @author Michael Remme
 */

public class JsonStoreObject implements IStoreObject<JsonObject> {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(JsonStoreObject.class);

  private JsonObject jsonObject;
  private IMapper mapper;
  private Object entity = null;
  private Collection<IObjectReference> objectReferences = new ArrayList<IObjectReference>();

  /**
   * Constructor
   * 
   * @param mapper
   *          the {@link IMapper} to be used
   * @param entity
   *          the entity to be used
   */
  public JsonStoreObject(IMapper mapper, Object entity) {
    if (mapper == null)
      throw new NullPointerException("Mapper must not be null");
    this.mapper = mapper;
    this.jsonObject = new JsonObject();
    this.entity = entity;
  }

  /**
   * Constructor
   * 
   * @param jsonObject
   *          the {@link JsonObject} read from the datastore
   * @param mapper
   *          the mapper to be used
   */
  public JsonStoreObject(JsonObject jsonObject, IMapper mapper) {
    if (mapper == null)
      throw new NullPointerException("Mapper must not be null");
    if (jsonObject == null)
      throw new NullPointerException("JsonObject must not be null");
    this.mapper = mapper;
    this.jsonObject = jsonObject;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.io.vertx.pojomapper.mapping.IStoreObject#get(de.braintags.io.vertx.pojomapper.mapping.IField)
   */
  @Override
  public Object get(IField field) {
    String colName = mapper.getTableInfo().getColumnInfo(field).getName();
    return jsonObject.getValue(colName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.io.vertx.pojomapper.mapping.IStoreObject#put(de.braintags.io.vertx.pojomapper.mapping.IField,
   * java.lang.Object)
   */
  @Override
  public IStoreObject<JsonObject> put(IField field, Object value) {
    IColumnInfo ci = field.getMapper().getTableInfo().getColumnInfo(field);
    if (ci == null) {
      throw new MappingException("Can't find columninfo for field " + field.getFullName());
    }
    jsonObject.put(ci.getName(), value);
    return this;
  }

  @Override
  public JsonObject getContainer() {
    return jsonObject;
  }

  /**
   * @return the mapper
   */
  public IMapper getMapper() {
    return mapper;
  }

  @Override
  public Object getEntity() {
    if (entity == null) {
      throw new NullPointerException(
          "Internal Entity is not initialized; call method MongoStoreObject.initToEntity first ");
    }
    return entity;
  }

  /**
   * Initialize the internal entity from the information previously read from the datastore.
   * 
   * @param handler
   */
  public final void initToEntity(Handler<AsyncResult<Void>> handler) {
    Object tmpObject = getMapper().getObjectFactory().createInstance(getMapper().getMapperClass());
    LOGGER.debug("start initToEntity");
    iterateFields(tmpObject, fieldResult -> {
      if (fieldResult.failed()) {
        handler.handle(fieldResult);
        return;
      }
      iterateObjectReferences(tmpObject, orResult -> {
        if (orResult.failed()) {
          handler.handle(orResult);
          return;
        }
        finishToEntity(tmpObject, handler);
        LOGGER.debug("finished initToEntity");
      });
    });
  }

  protected void finishToEntity(Object tmpObject, Handler<AsyncResult<Void>> handler) {
    this.entity = tmpObject;
    try {
      getMapper().executeLifecycle(AfterLoad.class, entity);
      handler.handle(Future.succeededFuture());
    } catch (Exception e) {
      handler.handle(Future.failedFuture(e));
    }
  }

  protected final void iterateFields(Object tmpObject, Handler<AsyncResult<Void>> handler) {
    LOGGER.debug("start iterateFields");
    ErrorObject<Void> error = new ErrorObject<Void>(handler);
    Set<String> fieldNames = getMapper().getFieldNames();
    CounterObject co = new CounterObject(fieldNames.size());
    for (String fieldName : fieldNames) {
      IField field = getMapper().getField(fieldName);
      LOGGER.debug("handling field " + field.getFullName());
      field.getPropertyMapper().fromStoreObject(tmpObject, this, field, result -> {
        if (result.failed()) {
          error.setThrowable(result.cause());
          return;
        }
        if (co.reduce()) {
          LOGGER.debug("field counter finished");
          handler.handle(Future.succeededFuture());
        }
      });
    }
  }

  protected void iterateObjectReferences(Object tmpObject, Handler<AsyncResult<Void>> handler) {
    LOGGER.debug("start iterateObjectReferences");
    if (getObjectReferences().isEmpty()) {
      LOGGER.debug("nothing to do");
      handler.handle(Future.succeededFuture());
      return;
    }
    ErrorObject<Void> error = new ErrorObject<Void>(handler);
    Collection<IObjectReference> refs = getObjectReferences();
    CounterObject co = new CounterObject(refs.size());
    for (IObjectReference ref : refs) {
      LOGGER.debug("handling object reference " + ref.getField().getFullName());
      ref.getField().getPropertyMapper().fromObjectReference(tmpObject, ref, result -> {
        if (result.failed()) {
          error.setThrowable(result.cause());
          return;
        }
        if (co.reduce()) {
          LOGGER.debug("object references finished");
          handler.handle(Future.succeededFuture());
        }
      });

    }
  }

  /**
   * Initialize the internal entity into the StoreObject
   * 
   * @param handler
   */
  public void initFromEntity(Handler<AsyncResult<Void>> handler) {
    ErrorObject<Void> error = new ErrorObject<Void>(handler);
    CounterObject co = new CounterObject(mapper.getFieldNames().size());
    for (String fieldName : mapper.getFieldNames()) {
      IField field = mapper.getField(fieldName);
      field.getPropertyMapper().intoStoreObject(entity, this, field, result -> {
        if (result.failed()) {
          error.setThrowable(result.cause());
        } else {
          if (co.reduce())
            handler.handle(Future.succeededFuture());
        }
      });
      if (error.isError()) {
        return;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.io.vertx.pojomapper.mapping.IStoreObject#getObjectReferences()
   */
  @Override
  public Collection<IObjectReference> getObjectReferences() {
    return objectReferences;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return mapper.getTableInfo().getName() + ": " + String.valueOf(jsonObject);
  }

}
