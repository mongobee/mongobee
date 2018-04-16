package com.github.mongobee.utils.proxy;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.NoOp;

import java.io.Serializable;

/**
 * @author dieppa
 * @since 04/04/2018
 */
class SerializableNoOp implements NoOp, Serializable {

  private static final long serialVersionUID = -7528524383141480009L;

  static final Callback SERIALIZABLE_INSTANCE = new SerializableNoOp();

  private SerializableNoOp() {
  }
}
