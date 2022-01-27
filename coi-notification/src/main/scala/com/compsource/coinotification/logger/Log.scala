package com.compsource.coinotification.logger

/**
 * Holds the method signature for structured logging
 */
trait Log {
  def debug(message: String)

  def debug(message: String, correlation_id: String, status: String, technical_details: String)

  def info(message: String)

  def info(message: String, correlation_id: String, status: String, technical_details: String)

  def warn(message: String)

  def warn(message: String, correlation_id: String, status: String, technical_details: String)

  def error(message: String)

  def error(message: String, correlation_id: String, status: String, technical_details: String)
}
