/*
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.io.vertx.pojomapper.testdatastore;

import org.junit.Test;

import de.braintags.io.vertx.pojomapper.mapping.IKeyGenerator;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.mapping.IStoreObject;
import de.braintags.io.vertx.pojomapper.mapping.IStoreObjectFactory;
import de.braintags.io.vertx.pojomapper.mapping.impl.keygen.DefaultKeyGenerator;
import de.braintags.io.vertx.pojomapper.testdatastore.mapper.KeyGeneratorMapper;
import de.braintags.io.vertx.pojomapper.testdatastore.mapper.NoKeyGeneratorMapper;
import de.braintags.io.vertx.util.ResultObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */

public class TestStoreObject extends DatastoreBaseTest {

  @Test
  public void testWithNullKeyGenerator(TestContext context) {
    NoKeyGeneratorMapper entity = new NoKeyGeneratorMapper();
    IMapper mapper = getDataStore(context).getMapperFactory().getMapper(entity.getClass());
    IKeyGenerator keyGen = mapper.getKeyGenerator();
    context.assertNull(keyGen, "keyGenerator must be null but is instance of " + String.valueOf(keyGen));

    entity.name = "testName";
    IStoreObject<?, ?> sto = createStoreObject(context, mapper, entity);
    if (getDataStore(context).getClass().getName().contains("Mongo")) {
      Object idValue = sto.get(mapper.getIdField());
      context.assertNull(idValue, "id must be null here: " + idValue);
      boolean hp = sto.hasProperty(mapper.getIdField());
      context.assertFalse(hp, "id-property should not exist here");
    } else {
      // mySQL creates ID later, not with StoreObject
    }
  }

  @Test
  public void testWithKeyGenerator(TestContext context) {
    KeyGeneratorMapper entity = new KeyGeneratorMapper();
    IMapper mapper = getDataStore(context).getMapperFactory().getMapper(entity.getClass());
    IKeyGenerator keyGen = mapper.getKeyGenerator();
    context.assertTrue(keyGen.getClass() == DefaultKeyGenerator.class,
        "keygenerator should be " + DefaultKeyGenerator.class.getName() + " but is " + keyGen.getClass().getName());
    entity.name = "testName";
    IStoreObject<?, ?> sto = createStoreObject(context, mapper, entity);
    if (getDataStore(context).getClass().getName().contains("Mongo")) {
      Object idValue = sto.get(mapper.getIdField());
      context.assertNotNull(idValue, "id value should have been generated by KeyGenerator locally: " + idValue);
      // should be a numeric value
      Integer.parseInt(String.valueOf(idValue));
    } else {
      // mySQL creates ID later, not with StoreObject
    }
  }

  @SuppressWarnings("unchecked")
  private IStoreObject<?, ?> createStoreObject(TestContext context, IMapper mapper, Object entity) {
    Async async = context.async();
    ResultObject<IStoreObject<?, ?>> ro = new ResultObject<>(null);
    IStoreObjectFactory<?> f = TestHelper.getDataStore().getMapperFactory().getStoreObjectFactory();
    f.createStoreObject(mapper, entity, result -> {
      if (result.failed()) {
        ro.setThrowable(result.cause());
      } else {
        ro.setResult(result.result());
      }
      async.complete();
    });
    async.await();
    if (ro.isError()) {
      throw ro.getRuntimeException();
    } else {
      return ro.getResult();
    }
  }
}
