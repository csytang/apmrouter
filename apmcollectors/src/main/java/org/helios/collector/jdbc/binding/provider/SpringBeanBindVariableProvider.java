/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.collector.jdbc.binding.provider;

import org.helios.collector.jdbc.binding.binder.IBinder;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <p>Title: SpringBeanBindVariableProvider</p>
 * <p>Description: Binds to a bind variable provider defined as a named Spring bean.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@BindVariableProvider(tokenKey="bean", defaultBinder=SimpleObjectBinder.class)
public class SpringBeanBindVariableProvider extends AbstractBindVariableProvider  {
	/** The provider this provider delegates to */
	protected IBindVariableProvider provider = null;
	/**
	 * Configures the spring bean underlying this provider.
	 * @param config The string configuration.
	 * @throws BindProviderConfigurationException
	 * @see org.helios.collectors.jdbc.binding.provider.AbstractBindVariableProvider#configure(java.lang.String)
	 */
	@Override
	public void configureProvider(String config) throws BindProviderConfigurationException {
		try {
			provider = (IBindVariableProvider)appContext.getBean(config, IBindVariableProvider.class);
			if(provider.getIBinder()==null) {
				provider.setBinder(binder);
			}
		} catch (Exception e) {
			throw new BindProviderConfigurationException("Failed to acquire provider delegate for [" + config + "]", e);
		}
	}
	
	/**
	 * @param sql
	 * @param bindToken
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#bind(java.lang.CharSequence, java.lang.String)
	 */
	public CharSequence bind(CharSequence sql, String bindToken) throws SQLException {
		return provider.bind(sql, bindToken);
	}
	
	/**
	 * @param ps
	 * @param bindSequence
	 * @return
	 * @throws SQLException
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#bind(java.sql.PreparedStatement, int)
	 */
	public int bind(PreparedStatement ps, int bindSequence) throws SQLException {
		return provider.bind(ps, bindSequence);
	}
	
	/**
	 * @return
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#getIBinder()
	 */
	public IBinder getIBinder() {
		return provider.getIBinder();
	}

	/**
	 * @return
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#getValue()
	 */
	public Object getValue() {
		return provider.getValue();
	}

	/**
	 * @param listener
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#registerListener(org.helios.collectors.jdbc.binding.provider.IBindVariableProviderListener)
	 */
	public void registerListener(IBindVariableProviderListener listener) {
		provider.registerListener(listener);
	}
	
	/**
	 * @param listener
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#removeListener(org.helios.collectors.jdbc.binding.provider.IBindVariableProviderListener)
	 */
	public void removeListener(IBindVariableProviderListener listener) {
		provider.removeListener(listener);
	}
	
	/**
	 * @param value
	 * @see org.helios.collectors.jdbc.binding.provider.IBindVariableProvider#setValue(java.lang.Object)
	 */
	public void setValue(Object value) {
		provider.setValue(value);
	}

}
