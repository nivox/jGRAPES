/*
 *  Copyright (c) 2011 Andrea Zito
 *
 *  This is free software; see lgpl-2.1.txt
 */
package jgrapes;

/**
 * Describe an exception of the GRAPES library
 *
 * @author Andrea Zito <zito.andrea@gmail.com>
 */
public class JGrapesException extends Exception{
  public JGrapesException(String msg) {
    super(msg);
  }

  public JGrapesException() {}
}