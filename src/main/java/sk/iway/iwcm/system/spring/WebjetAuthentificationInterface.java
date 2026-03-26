package sk.iway.iwcm.system.spring;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import sk.iway.iwcm.Identity;

/**
 * WebjetAuthentificationInterface.java
 *
 * Class WebjetAuthentificationInterface is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2020
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      25. 1. 2020 14:09
 * modified     25. 1. 2020 14:08
 */

public interface WebjetAuthentificationInterface {

    List<SimpleGrantedAuthority> create(Identity user);
}
