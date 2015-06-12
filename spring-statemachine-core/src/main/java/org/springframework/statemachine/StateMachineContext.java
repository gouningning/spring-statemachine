/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine;

import java.util.Map;

/**
 * {@code StateMachineContext} represents a current state of a state machine.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public interface StateMachineContext<S, E> {

	/**
	 * Gets the state machine.
	 *
	 * @return the state machine
	 */
	StateMachine<S, E> getStateMachine();

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	S getState();

	/**
	 * Gets the event.
	 *
	 * @return the event
	 */
	E getEvent();

	/**
	 * Gets the event headers.
	 *
	 * @return the event headers
	 */
	Map<String, Object> getEventHeaders();

	/**
	 * Gets the extended state.
	 *
	 * @return the extended state
	 */
	ExtendedState getExtendedState();

}
