/**
 * Cross-cutting building blocks shared by every domain module: base DTOs, the
 * error response shape, exception types and small utilities.
 *
 * <p>This package may not depend on any domain package. Dependencies point
 * inward only: {@code auth}, {@code feedback}, {@code vent} and {@code donation}
 * depend on {@code common}, never the reverse.
 */
package com.headheartfrees.common;
