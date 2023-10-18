package sk.iway.iwcm.system.jpa;

import java.util.Vector;

import org.eclipse.persistence.internal.databaseaccess.Accessor;
import org.eclipse.persistence.internal.sessions.AbstractSession;

import sk.iway.iwcm.PkeyGenerator;

/**
 * <p>
 * <b>Purpose</b>: Defines sequencing through using a SEQUENCE table.
 * <p>
 * <b>Description</b>
 * This is the default sequencing mechanism.
 *
 * JRASKA: WebJET override povodnej TableSequence. Namiesto defaultnej JPA
 * <code>TableSequence</code>, ktora pouziva tabulku SEQUENCE (pripadne nazov zadany v anotaciach)
 * sa na generovanie primarnych klucov pouzije <code>PkeyGenerator</code> z WJ.
 *
 * Tato funkcionalita je dosiahnuta odstranenim overridovanych protected metod <code>buildSelectQuery()</code> a
 * <code>buildUpdateQuery()</code> a overridom metod <code>getGeneratedValue()</code> (vracia PkeyGenerator hodnotu) a
 * <code>getGeneratedVector</code> (vracia <code>null</code>). Nakolko sa prepopulaciu stara <code>PkeyGenerator</code>, JPA nesmie
 * pri volani sekvencie pouzit <code>getGeneratedVector()</code>, toto dosiahneme overridom boolean metod
 * <code>shouldAcquireValueAfterInsert()</code> a <code>shouldUsePreallocation</code>, obidve vracaju <code>false</code>.
 */
@SuppressWarnings("rawtypes")
public class WJGenSequence extends org.eclipse.persistence.sequencing.TableSequence {

    public WJGenSequence() {
        super();
    }

    public WJGenSequence(String name) {
        super(name);
    }

   /**
    * Override metody, namiesto vratenia hodnot za pouzitia SEQUENCE tabulky sa vrati hodnota z
    * <code>PkeyGenerator.getNextValue(seqName)</code>
    *
    * Vo faze persistovania JPA entity, kde sa zo sekvencie generuje hodnota PK, sa do parametra
    * <b>seqName</b> ulozi hodnota z anotacneho parametra <code>pkColumnValue</code>
    */
   @Override
   public Object getGeneratedValue(Accessor accessor,	AbstractSession writeSession, String seqName)
	{
		return PkeyGenerator.getNextValue(seqName);
	}

	/**
	 * Override metody, vracia null, nakolko o prepopulaciu PK hodnot sa stara <code>PkeyGenerator</code>
	 */
   @Override
	public Vector getGeneratedVector(Accessor accessor, AbstractSession writeSession, String seqName, int size)
	{
		return null;
	}

	@Override
	public boolean shouldAcquireValueAfterInsert()
	{
		return false;
	}

	@Override
	public boolean shouldUseTransaction()
	{
		return false;
	}

    @Override
	public boolean shouldUsePreallocation()
	{
		return false;
	}
}
