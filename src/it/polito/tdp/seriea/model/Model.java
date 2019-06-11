package it.polito.tdp.seriea.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import it.polito.tdp.seriea.db.SerieADAO;

public class Model {
	
	private SerieADAO dao;
	private  List<Team> squadre;
	private Map<String,Team> squadreIdMap;
	private List<Season> stagioni;
	private Map<Integer,Season> stagioniIdMap;
	
	private Team squadraSelezionata ;
	private Map<Season, Integer> punteggi;
	
	private Graph <Season, DefaultWeightedEdge> grafo;
	
	private List<Season> percorsoBest;
	
	private List<Season> stagioniConsecutive;
	
	public Model() {
		dao= new SerieADAO();
		punteggi= new HashMap<>();
		this.squadre=dao.listTeams();
		this.stagioni=dao.listAllSeasons();
		squadreIdMap= new HashMap<>();
		for(Team t:this.squadre) {
			squadreIdMap.put(t.getTeam(), t);
		}
		
		
		this.stagioniIdMap=new HashMap<>();		
		for(Season s: this.stagioni) {
			this.stagioniIdMap.put(s.getSeason(), s);
		}
	}

	public List<Team> getSquadra() {

		return this.squadre;
	}
	
	public Map<Season,Integer> calcolaPuntegi(Team squadra){
		this.squadraSelezionata= squadra;
		
		this.punteggi= new HashMap<>();
		
		SerieADAO dao= new SerieADAO();
		List<Match> partite= dao.listMatchesForTeam(squadra, stagioniIdMap, squadreIdMap);
		
		for(Match m:partite) {			
			Season stagione= m.getSeason() ;
			
			int punti=0;
			
			if(m.getFtr().equals("D")) {
				punti = 1;
			}else {
				if ( (m.getHomeTeam().equals(squadra) && m.getFtr().equals("H")) || 
					(m.getAwayTeam().equals(squadra) && m.getFtr().equals("A") )){
					punti= 3;
				}
			}
			Integer attuale=punteggi.get(stagione);
			if(attuale==null)
				attuale=0;
			punteggi.put(stagione, attuale+punti);
		}
		return punteggi;
	}
	
	public Season calcolaAnnataOro() {
		
		//costruisco grafo
		
		this.grafo= new SimpleDirectedWeightedGraph<Season, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		Graphs.addAllVertices(grafo, punteggi.keySet());
		
		for (Season s1 : grafo.vertexSet()) {
			for (Season s2 : grafo.vertexSet()) {
				if (!s1.equals(s2)) {
					int punti1 = punteggi.get(s1);
					int punti2 = punteggi.get(s2);
					if (punti1 > punti2) {
						Graphs.addEdge(this.grafo, s2, s1, punti1 - punti2);
					} else {
						Graphs.addEdge(this.grafo, s1, s2, punti1 - punti2);
					}
				}
			}
		}		
		//trovo l'annata migliore
		
		Season migliore=null;
		int max=0 ;
		for(Season s: grafo.vertexSet()) {
			int valore= pesoStagione(s);
			if(valore > max) {
				max= valore;
				migliore= s;
			}
			
		}
		
		return migliore;	
	}

	private int pesoStagione(Season s) {
		
		int somma = 0;
		for (DefaultWeightedEdge edge : grafo.incomingEdgesOf(s)) {
			somma = (int) (somma + grafo.getEdgeWeight(edge));
		}
		for (DefaultWeightedEdge edge : grafo.outgoingEdgesOf(s)) {
			somma = (int) (somma - grafo.getEdgeWeight(edge));
		}
		return somma;
	}
	public List<Season> camminoVirtuoso() {
		
		//trova le stagioni consecutive
		stagioniConsecutive = new ArrayList<>(punteggi.keySet());		
		Collections.sort(stagioniConsecutive);
		
		//prepara le variabili utili per la ricorsione
		List<Season >parziale = new ArrayList<>();
		this.percorsoBest = new ArrayList<>();
		
		//itera al livello zero
		for(Season s: grafo.vertexSet()) {
			parziale.add(s);
			cerca(1,parziale);
			parziale.remove(0);			
		}
		return percorsoBest;
	}
	
	/*	RICORSIONE
	 * 
	 * Sol parziale: lista Season (vertici)
	 * Livello ricorsione: lunghezza della lista
	 * 
	 * Casi terminali: quando non trovo piu vertici da aggiungere 
	 * --> verifica se cammino ha lenght massima tra quelli visti fin'ora
	 * 
	 */
	
	public void cerca(int livello, List<Season > parziale) {
		
		boolean trovato= false;
		
		Season ultimo= parziale.get(livello-1);
		for(Season prossimo: Graphs.successorListOf(grafo, ultimo)) {
			if(!parziale.contains(prossimo)) {
				if(stagioniConsecutive.indexOf(ultimo)+1 == stagioniConsecutive.lastIndexOf(prossimo)) {
					// candidato accettabile
					parziale.add(prossimo) ;
					cerca(livello+1, parziale);
					parziale.remove(livello);
				}
			}
			
		}
		
		// valuta caso terminale
		if(!trovato) {
			if(parziale.size() > percorsoBest.size()) {
				percorsoBest = new ArrayList<Season>(parziale) ; //clona il Best
			}
		}
	}
}
