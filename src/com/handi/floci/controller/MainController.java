package com.handi.floci.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.Slider;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.json.simple.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.handi.floci.Main;
import com.handi.floci.modules.conceptclassification.HierarchyGenerator;
import com.handi.floci.modules.individualclassification.FuzzyOwl2toFuzzyDL;
import com.handi.floci.modules.individualclassification.IndividualClassificationDisplayer;
import com.handi.floci.modules.individualclassification.SimpleFuzzyReasoner;

import fuzzydl.Concept;
import fuzzydl.Individual;
import fuzzydl.KnowledgeBase;
import fuzzydl.MinInstanceQuery;
import fuzzydl.MinSatisfiableQuery;
import fuzzydl.Query;
import fuzzydl.exception.FuzzyOntologyException;
import fuzzydl.exception.InconsistentOntologyException;
import fuzzydl.milp.Solution;
import fuzzydl.parser.Parser;
import fuzzyowl2.FuzzyOwl2;

public class MainController {
	static final String ACCUEIL = "file:/D:/Development/Workspace/Java/FLOCI/src/com/handi/floci/modules/display/index.html";
	static final String VERTICAL_SHOW_PAGE = "file:/D:/Development/Workspace/Java/FLOCI/src/com/handi/floci/modules/display/dag.html";
	static final String HORIZONTAL_SHOW_PAGE = "file:/D:/Development/Workspace/Java/FLOCI/src/com/handi/floci/modules/display/show/dagH";
	static final String CIRCULAR_SHOW_PAGE = "file:/D:/Development/Workspace/Java/FLOCI/src/com/handi/floci/modules/display/show/dagC";
	
	static final int VERTICAL_SHOW = 0;
	static final int HORIZONTAL_SHOW = 1;
	static final int CIRCULAR_SHOW = 2;
	
	static final int REASONER_SFR = 0;
	static final int REASONER_FUZZYDL = 1;
	
	private String showPage = VERTICAL_SHOW_PAGE;
	private HierarchyGenerator hierarchyGenerator;
	private String ontologyIRI;
	public KnowledgeBase kb;
	
	private int reasonerType = REASONER_SFR;;
	private int affichageType;
	
	
    @FXML private TreeView<String> hierarchyTree;
    @FXML private ListView<OWLNamedIndividual> individualsList;
    @FXML private Button uploadOntologyButton; 
    @FXML private WebView webview;
    @FXML private CheckMenuItem checkSFR;
    @FXML private CheckMenuItem checkFuzzyDL;
    @FXML private CheckMenuItem checkVertical;
    @FXML private CheckMenuItem checkHorizontal;
    @FXML private CheckMenuItem checkCircular;
    
    @FXML private Slider zoomSlider;
    /**
     * The constructor.
     * The constructor is called before the initialize() method.
     */
    public MainController() {}
    
    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML private void initialize() { 
    	webview.getEngine().load(ACCUEIL);
    	
    	individualsList.setCellFactory(new Callback<ListView<OWLNamedIndividual>, ListCell<OWLNamedIndividual>>(){
			@Override
			public ListCell<OWLNamedIndividual> call(ListView<OWLNamedIndividual> param) {
				ListCell<OWLNamedIndividual> cell = new ListCell<OWLNamedIndividual>(){ 
                    @Override
                    protected void updateItem(OWLNamedIndividual individu, boolean bln) {
                        super.updateItem(individu, bln);
                        if (individu != null) {
                            setText(individu.getIRI().getFragment());
                        } else { // empty the cell if it has been reused
                        	setText(null);
                        }
                    }
                }; 
                return cell;
			}
    	});

    	
    	zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
    	      @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
    	          if (newValue != null) {
    	        	  int value = newValue.intValue();
    	        	  if(value % 10 == 0) {
	    	            webview.getEngine().executeScript("zoomia(" +value+")");
    	        	  }
    	          }
    	        }
    	      });
    }
    
    @FXML protected void uploadOntology(ActionEvent event) {   	
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Fichier Ontology OWL2");
        chooser.setInitialDirectory(new File("D:\\Development\\Tools\\Ontology"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichier Ontologie", "*.owl"),
                new FileChooser.ExtensionFilter("Tout les fichiers", "*.*"));
        File ontologyFile = chooser.showOpenDialog(uploadOntologyButton.getScene().getWindow());
       
		try {		
			// Load the Hierarchy Module
			hierarchyGenerator = new HierarchyGenerator(ontologyFile);
	        ontologyIRI = hierarchyGenerator.getOntology().getOntologyID().getOntologyIRI().toString();
			// Generate the Concept Hierarchy
			hierarchyGenerator.getConceptsHierarchy(hierarchyTree);
			
			// Rafrechir l'affichage
			setShowpage();
			System.out.println(showPage);
			webview.getEngine().load(showPage);
			
			kb = null;
			
			// Get Individuals
			showIndividuals();

		} catch (OWLOntologyCreationException e) {
			//e.printStackTrace();
		} catch (Exception e) {
			// Get Individuals
			showIndividuals();
			//e.printStackTrace();
		}
		
    }
    
    @SuppressWarnings("deprecation")
	@FXML protected void clickShowCreateIndividualWindow(ActionEvent event) {
    	OWLDataFactory factory = hierarchyGenerator.getOntologyManager().getOWLDataFactory();
    	
    	Optional<String> response = Dialogs.create()
    	        .owner(uploadOntologyButton.getScene().getWindow())
    	        .title("Ajouter un nouvel Individu")
    	        .masthead(null)
    	        .message("Nom du nouvel individu: ")
    	        .showTextInput("");
    	
    	if(response.isPresent()) {
    		String name = response.get();
    		if(!name.isEmpty()) {
    			name = name.replace(' ', '_');
	    		OWLNamedIndividual individu = factory.getOWLNamedIndividual(IRI.create(ontologyIRI +"#" + name));
	    		OWLClass thing = hierarchyGenerator.getOntologyManager().getOWLDataFactory().getOWLThing();    		
	    		OWLClassAssertionAxiom axiom = hierarchyGenerator.getOntologyManager().getOWLDataFactory().
	    				getOWLClassAssertionAxiom(thing, individu);
	    		
	    		hierarchyGenerator.getOntologyManager().addAxiom(hierarchyGenerator.getOntology(), axiom);
	    		
	    		showIndividualsWindow(event, individu, false);
    		} else {
        		Dialogs.create()
                .owner(uploadOntologyButton.getScene().getWindow())
                .title("Erreur")
                .masthead(null)
                .message("Ooops, Le nom de l'individu ne peut pas �tre vide!")
                .showError();
    		}
    	}
    }
    
    @SuppressWarnings("deprecation")
	@FXML protected void clickShowEditIndividualWindow(ActionEvent event) {
    	OWLNamedIndividual individu = null;
    	individu = individualsList.getSelectionModel().getSelectedItem();
    	
    	if (individu == null) {	
    		ObservableList<OWLNamedIndividual> list = this.hierarchyGenerator.getAllIndividuals();
    		if(!list.isEmpty()) individu = list.get(0);	
    	}
    	
    	if(individu != null) showIndividualsWindow(event, individu, true);
    	else {
			Dialogs.create()
            .owner(uploadOntologyButton.getScene().getWindow())
            .title("Erreur")
            .masthead(null)
            .message("Ooops, il n'existe aucun individus dans cette Ontologie!")
            .showError();
		}
    }
    
    private void showIndividualsWindow(ActionEvent event, OWLNamedIndividual individu, boolean editIndividual) {
    	try {
	    	// Load main layout.
	        FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/individuals_overview_layout.fxml"));
            Parent root = (Parent) loader.load();
            IndividualsOverviewController individualsController = (IndividualsOverviewController)loader.getController();
            individualsController.setParent(this);
            individualsController.setHierarchyGenerator(hierarchyGenerator);
            individualsController.setNamedIndividual(individu);
            individualsController.showIndividuals();
            individualsController.showAttributes();
            individualsController.showProperties();
            
	        Stage stage = new Stage();
	        stage.setScene(new Scene(root));
	        
	        stage.setTitle("Cr�ation d'un nouvel individu");
	        stage.initModality(Modality.WINDOW_MODAL);
	        stage.initOwner(uploadOntologyButton.getScene().getWindow());
	        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					try {
						hierarchyGenerator.reload();
					} catch (OWLOntologyCreationException e) {
						e.printStackTrace();
					}
					//showIndividuals();
				}
	        });
	        stage.show();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public void showIndividuals() {
    	individualsList.getSelectionModel().clearSelection();
    	individualsList.setItems(hierarchyGenerator.getAllIndividuals());
    	
    	showCleanHierarchyInWebview();
    }
    
    public void showCleanHierarchyInWebview() {
    	// empty the degrees JSON file
		try {
			FileWriter file = null;
			try {
				file = new FileWriter("D:\\Development\\Workspace\\Java\\FLOCI\\src\\com\\handi\\floci\\modules\\display\\degrees.json");
				file.write("");
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				try {
					file.flush();
					file.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			System.out.println(showPage);
			webview.getEngine().load(showPage);
		} catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    public void intitializeFuzzyDL(String ontologyFilePath) {
    	try {	
    		String outputPath = "D:/test.txt";
    		FuzzyOwl2toFuzzyDL parser = new FuzzyOwl2toFuzzyDL(ontologyFilePath, outputPath);
    		parser.translateOwl2Ontology();
    		
			kb = Parser.getKB(outputPath);
			Parser.reset();

			kb.solveKB();
			
		} catch (FuzzyOntologyException e) {
			e.printStackTrace();
		} catch (InconsistentOntologyException e) {
			e.printStackTrace();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    }
    
    @SuppressWarnings("deprecation")
	@FXML protected void classifySelectedIndividual(ActionEvent event) {
    	OWLNamedIndividual individu = individualsList.getSelectionModel().getSelectedItem();
    	if(individu != null) {
	    	if(reasonerType == REASONER_FUZZYDL) {
				if(kb == null) {
					intitializeFuzzyDL(hierarchyGenerator.getOntologyFilePath());
				}
				IndividualClassificationDisplayer displayer = new IndividualClassificationDisplayer(kb, hierarchyGenerator.getOntology());
				displayer.calculateMembershipFuzzyDL(individu);
			} else {
				IndividualClassificationDisplayer displayer = new IndividualClassificationDisplayer(hierarchyGenerator);
				displayer.calculateMembershipSFR(individu);
			}
	    	
	    	webview.getEngine().load(showPage);
    	} else {
    		Dialogs.create()
        	        .owner(uploadOntologyButton.getScene().getWindow())
        	        .title("Classification d'un individu")
        	        .message("Aucun Individu s�l�ctionn�!")
        	        .showInformation();
    	}
    }
    
    @SuppressWarnings("deprecation")
	@FXML protected void deleteSelectedIndividual(ActionEvent event) {
    	OWLNamedIndividual individu = individualsList.getSelectionModel().getSelectedItem();
    	if(individu != null) {
    		@SuppressWarnings("deprecation")
			Action response = Dialogs.create()
        	        .owner(uploadOntologyButton.getScene().getWindow())
        	        .title("Confirmer la suppression de l'individu")
        	        .masthead("La suppression d'un Individu est irr�versible!")
        	        .message("Etes vous sur de vouloir supprimer l'individu " + individu.getIRI().getFragment() +"?")
        	        .actions(Dialog.ACTION_OK, Dialog.ACTION_CANCEL)
        	        .showConfirm();

        	if (response == Dialog.ACTION_OK) {
	    		for(OWLAxiom axiom : this.hierarchyGenerator.getOntology().getAxioms(individu)) {
	    			hierarchyGenerator.getOntologyManager().removeAxiom(hierarchyGenerator.getOntology(), axiom);
	        	}
	    		
	    		try {
	    			hierarchyGenerator.getOntologyManager().saveOntology(hierarchyGenerator.getOntology());
	    			
	    			hierarchyGenerator.reload();
	    			
	    			kb = null;
	    			showIndividuals();
	    			
	    			showCleanHierarchyInWebview();
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
        	}
    	} else {
    		Dialogs.create()
        	        .owner(uploadOntologyButton.getScene().getWindow())
        	        .title("Classification d'un individu")
        	        .message("Aucun Individu s�l�ctionn�!")
        	        .showInformation();
    	}
    	
    }
    
    @FXML protected void changeReasonnerToSFR(ActionEvent event) {
    	checkFuzzyDL.setSelected(false);
    	checkSFR.setSelected(true);
    	reasonerType = REASONER_SFR;
    	individualsList.getSelectionModel().clearSelection();
    }
    @FXML protected void changeReasonnerToFuzzyDL(ActionEvent event) {
    	checkFuzzyDL.setSelected(true);
    	checkSFR.setSelected(false);
    	reasonerType = REASONER_FUZZYDL;
    	individualsList.getSelectionModel().clearSelection();
    }
    @FXML protected void changeAffichageToVertical(ActionEvent event) {
    	checkVertical.setSelected(true);
    	checkHorizontal.setSelected(false);
    	checkCircular.setSelected(false);
    	affichageType = VERTICAL_SHOW;
    	setShowpage();
    }
    @FXML protected void changeAffichageToHorizontal(ActionEvent event) {
    	checkVertical.setSelected(false);
    	checkHorizontal.setSelected(true);
    	checkCircular.setSelected(false);
    	affichageType = HORIZONTAL_SHOW;
    	setShowpage();
    }
    @FXML protected void changeAffichageToCircular(ActionEvent event) {
    	checkVertical.setSelected(false);
    	checkHorizontal.setSelected(false);
    	checkCircular.setSelected(true);
    	affichageType = CIRCULAR_SHOW;
    	setShowpage();
    }
    
    @FXML protected void returnToAccueilPage() {
    	webview.getEngine().load(ACCUEIL);
    }
    
    private void setShowpage() {
    	if(this.ontologyIRI == null) {
    		returnToAccueilPage();
    		return;
    	}
    	if(this.ontologyIRI.contains("neumo")) {
    		switch(this.affichageType) {
    			case 0: showPage = VERTICAL_SHOW_PAGE; break;
    			case 1: showPage = HORIZONTAL_SHOW_PAGE + "neumo.html"; break;
    			case 2: showPage = CIRCULAR_SHOW_PAGE + "neumo.html"; break;
    		}
    	} else if(this.ontologyIRI.contains("5484")) {
    		switch(this.affichageType) {
				case 0: showPage = VERTICAL_SHOW_PAGE; break;
				case 1: showPage = HORIZONTAL_SHOW_PAGE + "match.html"; break;
				case 2: showPage = CIRCULAR_SHOW_PAGE + "match.html"; break;
    		}
    	} else if(this.ontologyIRI.contains("wine")) {
    		switch(this.affichageType) {
				case 0: showPage = VERTICAL_SHOW_PAGE; break;
				case 1: showPage = HORIZONTAL_SHOW_PAGE + "wine.html"; break;
				case 2: showPage = CIRCULAR_SHOW_PAGE + "wine.html"; break;
    		}
    	} else {
    		showPage = VERTICAL_SHOW_PAGE; 
    	}
    	System.out.println(showPage);
		webview.getEngine().load(showPage);
    }
}
