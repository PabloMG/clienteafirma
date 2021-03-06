package es.gob.afirma.standalone.ui.preferences;

import static es.gob.afirma.standalone.ui.preferences.PreferencesManager.PREFERENCE_CADES_IMPLICIT;
import static es.gob.afirma.standalone.ui.preferences.PreferencesManager.PREFERENCE_CADES_POLICY_HASH;
import static es.gob.afirma.standalone.ui.preferences.PreferencesManager.PREFERENCE_CADES_POLICY_HASH_ALGORITHM;
import static es.gob.afirma.standalone.ui.preferences.PreferencesManager.PREFERENCE_CADES_POLICY_IDENTIFIER;
import static es.gob.afirma.standalone.ui.preferences.PreferencesManager.PREFERENCE_CADES_POLICY_QUALIFIER;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.signers.AdESPolicy;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.standalone.ui.preferences.PolicyPanel.PolicyItem;

final class PreferencesPanelCades extends JPanel {

	static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private static final long serialVersionUID = -2410844527428138817L;

	private static final String SIGN_FORMAT_CADES = "CAdES"; //$NON-NLS-1$

	/**
	 * Atributo que permite gestionar el bloqueo de preferencias.
	 */
	private boolean blocked = true;

	private static final AdESPolicy POLICY_CADES_PADES_AGE_1_9 = new AdESPolicy(
		"2.16.724.1.3.1.1.2.1.9", //$NON-NLS-1$
		"G7roucf600+f03r/o0bAOQ6WAs0=", //$NON-NLS-1$
		"SHA1", //$NON-NLS-1$
		"https://sede.060.gob.es/politica_de_firma_anexo_1.pdf" //$NON-NLS-1$
	);

	private PolicyPanel cadesPolicyDlg;

	/**
	 * Atributo que representa la etiqueta de la pol&iacute;tica seleccionada en
	 * el di&aacute;logo
	 */
	private JLabel policyLabel;

	private final JCheckBox cadesImplicit = new JCheckBox(SimpleAfirmaMessages.getString("PreferencesPanel.1")); //$NON-NLS-1$

	PreferencesPanelCades(final KeyListener keyListener,
						  final ModificationListener modificationListener,
						  final boolean blocked) {

		this.blocked = blocked;
		createUI(keyListener, modificationListener);
	}

	void createUI(final KeyListener keyListener,
				  final ModificationListener modificationListener) {

        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.gridy = 0;

        loadPreferences();

        loadCadesPolicy();

    	this.cadesPolicyDlg.setModificationListener(modificationListener);
    	this.cadesPolicyDlg.setKeyListener(keyListener);

        ///////////// Panel Policy ////////////////

        final JPanel policyConfigPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		policyConfigPanel.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createTitledBorder(SimpleAfirmaMessages.getString("PreferencesPanel.153")) //$NON-NLS-1$
			)
		);

		final JButton policyConfigButton = new JButton(
			SimpleAfirmaMessages.getString("PreferencesPanel.150") //$NON-NLS-1$
		);

		this.policyLabel = new JLabel(this.cadesPolicyDlg.getSelectedPolicyName());
		this.policyLabel.setLabelFor(policyConfigButton);

		policyConfigButton.setMnemonic('P');
		policyConfigButton.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent ae) {
					changeCadesPolicyDlg(getParent());
				}
			}
		);
		policyConfigButton.getAccessibleContext().setAccessibleDescription(
			SimpleAfirmaMessages.getString("PreferencesPanel.151") //$NON-NLS-1$
		);

		policyConfigButton.setEnabled(!this.blocked);
		policyConfigPanel.add(this.policyLabel);
		policyConfigPanel.add(policyConfigButton);

        ///////////// Fin Panel Policy ////////////////

		c.gridy++;

        add(policyConfigPanel, c);

	    final FlowLayout fLayout = new FlowLayout(FlowLayout.LEADING);
	    final JPanel signatureMode = new JPanel(fLayout);
	    signatureMode.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createTitledBorder(
				SimpleAfirmaMessages.getString("PreferencesPanel.69")) //$NON-NLS-1$
			)
		);
	    this.cadesImplicit.getAccessibleContext().setAccessibleDescription(
    		SimpleAfirmaMessages.getString("PreferencesPanel.45") //$NON-NLS-1$
		);
	    this.cadesImplicit.setMnemonic('i');
	    this.cadesImplicit.addItemListener(modificationListener);
	    this.cadesImplicit.addKeyListener(keyListener);
	    this.cadesImplicit.setEnabled(this.blocked);
	    signatureMode.add(this.cadesImplicit);

	    c.gridy++;
	    add(signatureMode, c);

	    c.gridy++;
	    c.weighty = 1.0;
	    add(new JPanel(), c);

		// Panel para el boton de restaurar la configuracion
		final JPanel panelGeneral = new JPanel(new FlowLayout(FlowLayout.TRAILING));

		final JButton restoreConfigButton = new JButton(SimpleAfirmaMessages.getString("PreferencesPanel.147") //$NON-NLS-1$
		);

		restoreConfigButton.setMnemonic('R');
		restoreConfigButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent ae) {
				if (AOUIFactory.showConfirmDialog(getParent(), SimpleAfirmaMessages.getString("PreferencesPanel.157"), //$NON-NLS-1$
						SimpleAfirmaMessages.getString("PreferencesPanel.139"), //$NON-NLS-1$
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

					loadDefaultPreferences();

				}
			}
		});
		restoreConfigButton.getAccessibleContext()
				.setAccessibleDescription(SimpleAfirmaMessages.getString("PreferencesPanel.136") //$NON-NLS-1$
		);

	    c.gridy++;
	    c.weighty = 0.0;
		panelGeneral.add(restoreConfigButton, c);

		c.gridy++;

		add(panelGeneral, c);

	}

	void checkPreferences() throws AOException {

		loadCadesPolicy();

		final AdESPolicy p = this.cadesPolicyDlg.getSelectedPolicy();
		if (p != null) {
			// No nos interesa el resultado, solo si construye sin excepciones
			try {
				new Oid(p.getPolicyIdentifier().replace("urn:oid:", "")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (final GSSException e) {
				throw new AOException("El identificador debe ser un OID", e); //$NON-NLS-1$
			}
		}
	}

	void savePreferences() {
		PreferencesManager.putBoolean(PREFERENCE_CADES_IMPLICIT, this.cadesImplicit.isSelected());
		final AdESPolicy cadesPolicy = this.cadesPolicyDlg.getSelectedPolicy();
		if (cadesPolicy != null) {
			PreferencesManager.put(PREFERENCE_CADES_POLICY_IDENTIFIER, cadesPolicy.getPolicyIdentifier());
			PreferencesManager.put(PREFERENCE_CADES_POLICY_HASH, cadesPolicy.getPolicyIdentifierHash());
			PreferencesManager.put(PREFERENCE_CADES_POLICY_HASH_ALGORITHM, cadesPolicy.getPolicyIdentifierHashAlgorithm());
			if (cadesPolicy.getPolicyQualifier() != null) {
				PreferencesManager.put(PREFERENCE_CADES_POLICY_QUALIFIER, cadesPolicy.getPolicyQualifier().toString());
			}
			else {
				PreferencesManager.remove(PREFERENCE_CADES_POLICY_QUALIFIER);
			}
		}
		else {
			PreferencesManager.remove(PREFERENCE_CADES_POLICY_IDENTIFIER);
			PreferencesManager.remove(PREFERENCE_CADES_POLICY_HASH);
			PreferencesManager.remove(PREFERENCE_CADES_POLICY_HASH_ALGORITHM);
			PreferencesManager.remove(PREFERENCE_CADES_POLICY_QUALIFIER);
		}
		this.cadesPolicyDlg.saveCurrentPolicy();
	}

	void loadPreferences() {
		this.cadesImplicit.setSelected(PreferencesManager.getBooleanPreference(PREFERENCE_CADES_IMPLICIT, true));

        final List<PolicyPanel.PolicyItem> cadesPolicies = new ArrayList<>();
        cadesPolicies.add(
    		new PolicyItem(
				SimpleAfirmaMessages.getString("PreferencesPanel.73"), //$NON-NLS-1$
				POLICY_CADES_PADES_AGE_1_9
			)
		);

        this.cadesPolicyDlg = new PolicyPanel(
    		SIGN_FORMAT_CADES,
    		cadesPolicies,
    		getCadesPreferedPolicy(),
    		this.blocked
        );

        revalidate();
        repaint();
	}

	void loadDefaultPreferences() {

		final List<PolicyPanel.PolicyItem> cadesPolicies = new ArrayList<>();
        cadesPolicies.add(
    		new PolicyItem(
        		SimpleAfirmaMessages.getString("PreferencesPanel.73"), //$NON-NLS-1$
        		POLICY_CADES_PADES_AGE_1_9
    		)
		);

        this.cadesPolicyDlg = new PolicyPanel(
        		SIGN_FORMAT_CADES,
        		cadesPolicies,
        		getCadesDefaultPolicy(),
        		this.blocked
    		);

		this.policyLabel.setText(this.cadesPolicyDlg.getSelectedPolicyName());

        revalidate();
        repaint();
	}

	/** Obtiene la configuraci&oacute;n de politica de firma CAdES establecida actualmente.
	 * @return Pol&iacute;tica de firma configurada. */
	private static AdESPolicy getCadesPreferedPolicy() {

		if (PreferencesManager.get(PREFERENCE_CADES_POLICY_IDENTIFIER, null) == null) {
			return null;
		}
		try {
			return new AdESPolicy(
					PreferencesManager.get(PREFERENCE_CADES_POLICY_IDENTIFIER, null),
					PreferencesManager.get(PREFERENCE_CADES_POLICY_HASH, null),
					PreferencesManager.get(PREFERENCE_CADES_POLICY_HASH_ALGORITHM, null),
					PreferencesManager.get(PREFERENCE_CADES_POLICY_QUALIFIER, null)
					);
		}
		catch (final Exception e) {
			Logger.getLogger("es.gob.afirma").severe("Error al recuperar la politica CAdES guardada en preferencias: " + e); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	/** Obtiene la configuraci&oacute;n de politica de firma CAdES por defecto.
	 * @return Pol&iacute;tica de firma configurada. */
	private AdESPolicy getCadesDefaultPolicy() {

		AdESPolicy adesPolicy = null;

		loadCadesPolicy();

		if (this.blocked) {

			// blocked = true, luego no pueden alterarse las propiedades:
			// devolvemos las preferencias almacenadas actualmente

			adesPolicy = this.cadesPolicyDlg.getSelectedPolicy();

		} else {

			// blocked = false, luego se pueden alterar las propiedades:
			// devolvemos las preferencias por defecto
			try {

				if (PreferencesManager.getPreference(PREFERENCE_CADES_POLICY_IDENTIFIER, null) == null
						|| "".equals(PreferencesManager.getPreference(PREFERENCE_CADES_POLICY_IDENTIFIER, null))) { //$NON-NLS-1$
					this.cadesPolicyDlg.loadPolicy(null);
				} else {

					this.cadesPolicyDlg
							.loadPolicy(new AdESPolicy(PreferencesManager.get(PREFERENCE_CADES_POLICY_IDENTIFIER, null),
									PreferencesManager.get(PREFERENCE_CADES_POLICY_HASH, null),
									PreferencesManager.get(PREFERENCE_CADES_POLICY_HASH_ALGORITHM, null),
									PreferencesManager.get(PREFERENCE_CADES_POLICY_QUALIFIER, null)));
				}
			} catch (final Exception e) {
				Logger.getLogger("es.gob.afirma") //$NON-NLS-1$
						.severe("Error al recuperar la politica CAdES guardada en preferencias: " + e); //$NON-NLS-1$

			}
		}

		return adesPolicy;

	}


	/**
	 * Carga el panel de pol&iacute;tica con las preferencias guardadas
	 */
	private void loadCadesPolicy() {
		// Si el panel no está cargado lo obtengo de las preferencias guardadas
		if (this.cadesPolicyDlg == null) {
			final List<PolicyPanel.PolicyItem> cadesPolicies = new ArrayList<>();
			cadesPolicies.add(new PolicyItem(SimpleAfirmaMessages.getString("PreferencesPanel.73"), //$NON-NLS-1$
					POLICY_CADES_PADES_AGE_1_9));

			this.cadesPolicyDlg = new PolicyPanel(SIGN_FORMAT_CADES, cadesPolicies, getCadesPreferedPolicy(), this.blocked);
		}
	}

	/**
	 * Di&aacute;logo para cambair la configuracion de la pol&iacute;tica
	 *
	 * @param container
	 *            Contenedor en el que se define el di&aacute;logo.
	 */
	public void changeCadesPolicyDlg(final Container container) {

		// Cursor en espera
		container.setCursor(new Cursor(Cursor.WAIT_CURSOR));

		// Cursor por defecto
		container.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

		loadCadesPolicy();

		if (AOUIFactory.showConfirmDialog(container, this.cadesPolicyDlg,
				SimpleAfirmaMessages.getString("PolicyDialog.0"), //$NON-NLS-1$
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.DEFAULT_OPTION) == JOptionPane.OK_OPTION) {

			try {
				checkPreferences();

				this.policyLabel.setText(this.cadesPolicyDlg.getSelectedPolicyName());

				final AdESPolicy cadesPolicy = this.cadesPolicyDlg.getSelectedPolicy();
				if (cadesPolicy != null) {
					PreferencesManager.put(PREFERENCE_CADES_POLICY_IDENTIFIER, cadesPolicy.getPolicyIdentifier());
					PreferencesManager.put(PREFERENCE_CADES_POLICY_HASH, cadesPolicy.getPolicyIdentifierHash());
					PreferencesManager.put(PREFERENCE_CADES_POLICY_HASH_ALGORITHM,
							cadesPolicy.getPolicyIdentifierHashAlgorithm());
					if (cadesPolicy.getPolicyQualifier() != null) {
						PreferencesManager.put(PREFERENCE_CADES_POLICY_QUALIFIER,
								cadesPolicy.getPolicyQualifier().toString());
					} else {
						PreferencesManager.remove(PREFERENCE_CADES_POLICY_QUALIFIER);
					}
				} else {
					PreferencesManager.remove(PREFERENCE_CADES_POLICY_IDENTIFIER);
					PreferencesManager.remove(PREFERENCE_CADES_POLICY_HASH);
					PreferencesManager.remove(PREFERENCE_CADES_POLICY_HASH_ALGORITHM);
					PreferencesManager.remove(PREFERENCE_CADES_POLICY_QUALIFIER);
				}

				this.cadesPolicyDlg.saveCurrentPolicy();

			} catch (final Exception e) {

				AOUIFactory.showErrorMessage(this,
						"<html><p>" + SimpleAfirmaMessages.getString("PreferencesPanel.38") + ":<br>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								+ e.getLocalizedMessage() + "</p></html>", //$NON-NLS-1$
						SimpleAfirmaMessages.getString("SimpleAfirma.7"), //$NON-NLS-1$
						JOptionPane.ERROR_MESSAGE);
				changeCadesPolicyDlg(container);

			}

		}

		this.cadesPolicyDlg = null;

	}

}
