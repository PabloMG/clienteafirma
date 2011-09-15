/*
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un applet de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Ministerio de la Presidencia, Gobierno de Espana
 * Este fichero se distribuye bajo licencia GPL version 3 segun las
 * condiciones que figuran en el fichero 'licence' que se acompana.  Si se   distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 */
package es.gob.afirma.ui.wizardUtils;

import javax.swing.JTextPane;

import es.gob.afirma.ui.utils.GeneralConfig;
import es.gob.afirma.ui.utils.Messages;
import es.gob.afirma.ui.utils.Utils;

/**
 * Clase para generar bloques de texto
 */
public class PanelesTexto {

	public PanelesTexto () {
		
	}

	/**
	 * Genera un panel con un bloque de texto
	 * @param bundleTexto	Texto a buscar en el ResourceBundle para el panel
	 * @param opaco	Indica si el fondo debe ser blanco (true) o gris (false)
	 * @return	Panel con un bloque de texto
	 */
	public static JTextPane generarPanelTexto(String bundleTexto, boolean opaco) {
        // Bloque de texto
        JTextPane bloqueTexto = new JTextPane();
        bloqueTexto.setEditable(false);
        bloqueTexto.setText(Messages.getString(bundleTexto));
        bloqueTexto.setOpaque(opaco);
        bloqueTexto.setBorder(null);
        bloqueTexto.setFocusable(true);
        if(GeneralConfig.isRemarked()){
        	Utils.remarcar(bloqueTexto);
        }
        
        return bloqueTexto;
	}
}
