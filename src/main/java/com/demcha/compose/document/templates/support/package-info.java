/**
 * Domain-grouped helpers, scene composers, mappers, and composition adapters
 * used by the canonical document template layer.
 *
 * <p>This package sits between the public template contracts in
 * {@link com.demcha.compose.document.templates.api} and the low-level semantic
 * authoring/runtime layers in {@code com.demcha.compose.document.*}. Most
 * applications should consume the public template interfaces instead of these
 * support types directly. Common composition primitives live in
 * {@code support.common}; domain scene composers live in packages such as
 * {@code support.cv}, {@code support.business}, and {@code support.schedule}.</p>
 */
package com.demcha.compose.document.templates.support;
