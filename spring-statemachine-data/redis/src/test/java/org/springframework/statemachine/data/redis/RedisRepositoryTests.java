/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine.data.redis;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.statemachine.TestUtils.doSendEventAndConsumeAll;
import static org.springframework.statemachine.TestUtils.doStartAndAssert;
import static org.springframework.statemachine.TestUtils.resolveMachine;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.AbstractRepositoryTests;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.transition.TransitionKind;

/**
 * Redis repository config tests.
 *
 * @author Janne Valkealahti
 */
@EnabledOnRedis
public class RedisRepositoryTests extends AbstractRepositoryTests {

	@Override
	protected void cleanInternal() {
		AnnotationConfigApplicationContext c = new AnnotationConfigApplicationContext();
		c.register(TestConfig.class);
		c.refresh();
		KeyValueTemplate kvTemplate = c.getBean(KeyValueTemplate.class);
		kvTemplate.delete(RedisRepositoryAction.class);
		kvTemplate.delete(RedisRepositoryGuard.class);
		kvTemplate.delete(RedisRepositoryState.class);
		kvTemplate.delete(RedisRepositoryTransition.class);
		c.close();
	}

	@Test
	public void testPopulate1() {
		context.register(getRegisteredClasses());
		context.register(Config2.class);
		context.refresh();

		RedisStateRepository stateRepository = context.getBean(RedisStateRepository.class);
		RedisTransitionRepository transitionRepository = context.getBean(RedisTransitionRepository.class);
		assertThat(stateRepository.count(), is(3l));
		assertThat(transitionRepository.count(), is(3l));

		List<RedisRepositoryState> states = new ArrayList<>();
		stateRepository.findAll().iterator().forEachRemaining(states::add);
		List<RedisRepositoryTransition> transitions = new ArrayList<>();
		transitionRepository.findAll().iterator().forEachRemaining(transitions::add);
		assertThat(states.size(), is(3));
		assertThat(transitions.size(), is(3));
		RedisRepositoryTransition transition1 = transitions.get(0);
		assertThat(transition1.getSource(), notNullValue());
		assertThat(transition1.getTarget(), notNullValue());
	}

	@Test
	public void testRepository1() {
		context.register(getRegisteredClasses());
		context.refresh();

		RedisStateRepository statesRepository = context.getBean(RedisStateRepository.class);
		RedisRepositoryState stateS1 = new RedisRepositoryState("S1");
		RedisRepositoryState stateS2 = new RedisRepositoryState("S2");
		assertThat(statesRepository.count(), is(0l));

		statesRepository.save(stateS1);
		statesRepository.save(stateS2);
		assertThat(statesRepository.count(), is(2l));

		RedisTransitionRepository transitionsRepository = context.getBean(RedisTransitionRepository.class);
		RedisRepositoryTransition transition = new RedisRepositoryTransition(stateS1, stateS2, "E1");
		transition.setKind(TransitionKind.EXTERNAL);
		transitionsRepository.save(transition);

		assertThat(statesRepository.count(), is(2l));

		RedisRepositoryTransition transition2 = transitionsRepository.findAll().iterator().next();
		assertThat(transition2.getSource().getState(), is("S1"));
		assertThat(transition2.getTarget().getState(), is("S2"));
		assertThat(transition2.getEvent(), is("E1"));
		assertThat(transition2.getKind(), is(TransitionKind.EXTERNAL));

		List<RedisRepositoryState> findByMachineId = statesRepository.findByMachineId("");
		assertThat(findByMachineId.size(), is(2));

		context.close();
	}

	@Test
	public void testStateMachinePersistWithStrings() {
		context.register(TestConfig.class, ConfigWithStrings.class);
		context.refresh();

		StateMachine<String, String> stateMachine = resolveMachine(context);
		doStartAndAssert(stateMachine);
		assertThat(stateMachine.getState().getId(), is("S1"));
		doSendEventAndConsumeAll(stateMachine, "E1");
		assertThat(stateMachine.getState().getId(), is("S2"));
		doSendEventAndConsumeAll(stateMachine, "E2");
		assertThat(stateMachine.getState().getId(), is("S1"));
	}

	@Test
	public void testStateMachinePersistWithEnums() {
		context.register(TestConfig.class, ConfigWithEnums.class);
		context.refresh();

		StateMachine<PersistTestStates, PersistTestEvents> stateMachine = resolveMachine(context);
		doStartAndAssert(stateMachine);
		assertThat(stateMachine.getState().getId(), is(PersistTestStates.S1));
		doSendEventAndConsumeAll(stateMachine, PersistTestEvents.E1);
		assertThat(stateMachine.getState().getId(), is(PersistTestStates.S2));
		doSendEventAndConsumeAll(stateMachine, PersistTestEvents.E2);
		assertThat(stateMachine.getState().getId(), is(PersistTestStates.S1));
	}

	@Override
	protected Class<?>[] getRegisteredClasses() {
		return new Class<?>[] { TestConfig.class };
	}

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}

	@EnableAutoConfiguration
	static class TestConfig {
	}

	@Configuration
	@EnableStateMachine
	public static class ConfigWithStrings extends StateMachineConfigurerAdapter<String, String> {

		@Autowired
		private RedisStateMachineRepository redisStateMachineRepository;

		@Override
		public void configure(StateMachineConfigurationConfigurer<String, String> config) throws Exception {
			config
				.withConfiguration()
					.machineId("xxx1")
				.and()
				.withPersistence()
					.runtimePersister(stateMachineRuntimePersister());
		}

		@Override
		public void configure(StateMachineStateConfigurer<String, String> states) throws Exception {
			states
				.withStates()
					.initial("S1")
					.state("S2");
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<String, String> transitions) throws Exception {
			transitions
				.withExternal()
					.source("S1")
					.target("S2")
					.event("E1")
					.and()
				.withExternal()
					.source("S2")
					.target("S1")
					.event("E2");
		}

		@Bean
		public StateMachineRuntimePersister<String, String, String> stateMachineRuntimePersister() {
			return new RedisPersistingStateMachineInterceptor<>(redisStateMachineRepository);
		}
	}

	@Configuration
	@EnableStateMachine
	public static class ConfigWithEnums extends StateMachineConfigurerAdapter<PersistTestStates, PersistTestEvents> {

		@Autowired
		private RedisStateMachineRepository redisStateMachineRepository;

		@Override
		public void configure(StateMachineConfigurationConfigurer<PersistTestStates, PersistTestEvents> config) throws Exception {
			config
				.withConfiguration()
					.machineId("xxx2")
				.and()
				.withPersistence()
					.runtimePersister(stateMachineRuntimePersister());
		}

		@Override
		public void configure(StateMachineStateConfigurer<PersistTestStates, PersistTestEvents> states) throws Exception {
			states
				.withStates()
					.initial(PersistTestStates.S1)
					.state(PersistTestStates.S2);
		}

		@Override
		public void configure(StateMachineTransitionConfigurer<PersistTestStates, PersistTestEvents> transitions) throws Exception {
			transitions
				.withExternal()
					.source(PersistTestStates.S1)
					.target(PersistTestStates.S2)
					.event(PersistTestEvents.E1)
					.and()
				.withExternal()
					.source(PersistTestStates.S2)
					.target(PersistTestStates.S1)
					.event(PersistTestEvents.E2);
		}

		@Bean
		public StateMachineRuntimePersister<PersistTestStates, PersistTestEvents, String> stateMachineRuntimePersister() {
			return new RedisPersistingStateMachineInterceptor<>(redisStateMachineRepository);
		}
	}

	public enum PersistTestStates {
		S1, S2;
	}

	public enum PersistTestEvents {
		E1, E2;
	}
}
