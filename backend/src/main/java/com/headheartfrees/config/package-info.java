/**
 * Application-wide Spring configuration: HTTP security, CORS, OpenAPI, rate
 * limiting and Jackson.
 *
 * <p>Configuration classes wire beans together but hold no business logic. Rules
 * that belong to a domain (who may moderate feedback, how a token is minted)
 * live in that domain's service, not here.
 */
package com.headheartfrees.config;
