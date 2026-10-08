/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;

/**
 * Singleton scheduler that periodically triggers the transfer-status check on every
 * active {@link DestinationLogic}.
 *
 * <p>
 * The polling must live in a singleton because {@code @Scheduled} is only processed on
 * singleton beans, whereas {@link DestinationLogic} is UI-scoped so that each user
 * session keeps its own view state. UI-scoped logics register themselves on creation and
 * unregister on destruction.
 */
@Service
@Generated()
public class DestinationStatusScheduler {

	private final Set<DestinationLogic> activeDestinationLogics = new CopyOnWriteArraySet<>();

	public void register(DestinationLogic destinationLogic) {
		activeDestinationLogics.add(destinationLogic);
	}

	public void unregister(DestinationLogic destinationLogic) {
		activeDestinationLogics.remove(destinationLogic);
	}

	/**
	 * Check activity on the forward node of every registered destination logic.
	 */
	@Scheduled(fixedRate = 1000)
	public void checkStatusTransfers() {
		activeDestinationLogics.forEach(DestinationLogic::checkStatusTransfers);
	}

}