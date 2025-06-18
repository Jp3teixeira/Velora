package controller;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.Moeda;
import model.Ordem;
import model.Portfolio;
import model.Transacao;
import Repository.OrdemRepository;
import Repository.PortfolioRepository;
import Repository.TransacaoRepository;
import utils.MarketSimulator;
import utils.TradeService;
import utils.SessaoAtual;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HomeController implements Initializable {

    @FXML private VBox chartSection;

    @FXML private ToggleGroup orderTypeGroup;
    @FXML private ToggleButton marketToggle, limitToggle;
    @FXML private Label assetLabel;
    @FXML private TextField priceField, quantityField;
    @FXML private Button buyButton, sellButton;

    @FXML private ToggleGroup tableToggleGroup;
    @FXML private ToggleButton btnPosicoes, btnOrdens, btnHistorico;
    @FXML private StackPane tableContainer;
    @FXML private TableView<Portfolio> openPositionsTable;
    @FXML private TableView<Ordem>     openOrdersTable;
    @FXML private TableView<Transacao> historyTable;

    private final PortfolioRepository portfolioRepo = new PortfolioRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();
    private final OrdemRepository     ordemRepo;
    private Connection conn;
    private Moeda ativoAtual;

    public HomeController() {
        try {
            conn = Database.DBConnection.getConnection();
            ordemRepo = new OrdemRepository(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // inicia simulador
        MarketSimulator.startSimulador();

        marketToggle.setOnAction(e -> priceField.setDisable(true));
        limitToggle .setOnAction(e -> priceField.setDisable(false));

        // gráfico
        List<Moeda> lista = List.copyOf(MarketSimulator.getMoedasSimuladas().values());
        if (!lista.isEmpty()) {
            ativoAtual = lista.get(0);
            assetLabel.setText("Ativo: " + ativoAtual.getNome() + " (" + ativoAtual.getSimbolo()+")");
            LineChart<String, Number> chart = criarChartParaMoeda(ativoAtual.getIdMoeda());
            chartSection.getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);
        }

        configurarTabelaPortfolio();
        carregarPortfolio();

        configurarTabelaOrdens();
        carregarOrdens();

        configurarTabelaHistorico();
        carregarHistorico();

        // toggles de tabela
        tableToggleGroup.selectedToggleProperty().addListener((obs,o,n)->{
            openPositionsTable.setVisible(n==btnPosicoes);
            openOrdersTable   .setVisible(n==btnOrdens);
            historyTable      .setVisible(n==btnHistorico);
        });

        buyButton.setOnAction(e->executar("compra"));
        sellButton.setOnAction(e->executar("venda"));
    }

    private void configurarTabelaPortfolio() {
        openPositionsTable.getColumns().clear();
        var c1 = new TableColumn<Portfolio,String>("Ativo");
        c1.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getMoeda().getNome()));
        var c2 = new TableColumn<Portfolio,String>("Quantidade");
        c2.setCellValueFactory(c->{
            BigDecimal q=c.getValue().getQuantidade();
            return new SimpleStringProperty(q.setScale(8,RoundingMode.HALF_UP).toPlainString());
        });
        var c3 = new TableColumn<Portfolio,String>("Preço Médio (€)");
        c3.setCellValueFactory(c->{
            BigDecimal m=c.getValue().getPrecoMedioCompra();
            return new SimpleStringProperty(m.setScale(2,RoundingMode.HALF_UP).toPlainString());
        });
        var c4 = new TableColumn<Portfolio,String>("Valor Mercado (€)");
        c4.setCellValueFactory(c->{
            BigDecimal tot=c.getValue().getQuantidade()
                    .multiply(c.getValue().getMoeda().getValorAtual())
                    .setScale(2,RoundingMode.HALF_UP);
            return new SimpleStringProperty(tot.toPlainString());
        });
        openPositionsTable.getColumns().setAll(c1,c2,c3,c4);
        openPositionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void carregarPortfolio(){
        int uid=SessaoAtual.utilizadorId;
        var lst=portfolioRepo.listarPorUtilizador(uid);
        openPositionsTable.setItems(FXCollections.observableArrayList(lst));
    }

    private void configurarTabelaOrdens(){
        openOrdersTable.getColumns().clear();
        var c1=new TableColumn<Ordem,String>("Ativo");
        c1.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getMoeda().getNome()));
        var c2=new TableColumn<Ordem,String>("Qtd");
        c2.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getQuantidade().toPlainString()));
        var c3=new TableColumn<Ordem,String>("Preço (€)");
        c3.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getPrecoUnitarioEur().toPlainString()));
        var c4=new TableColumn<Ordem,String>("Modo");
        c4.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getModo()));
        var c5=new TableColumn<Ordem,String>("Tipo");
        c5.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getTipoOrdem()));
        openOrdersTable.getColumns().setAll(c1,c2,c3,c4,c5);
        openOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void carregarOrdens(){
        int uid=SessaoAtual.utilizadorId;
        try {
            var ords = ordemRepo.listarOrdensPendentesPorUsuario(uid)
                    .stream()
                    .filter(o->o.getTipoOrdem().equalsIgnoreCase("ativa"))
                    .collect(Collectors.toList());
            openOrdersTable.setItems(FXCollections.observableArrayList(ords));
        } catch(SQLException ex){
            new Alert(Alert.AlertType.ERROR,"Erro a carregar ordens").showAndWait();
        }
    }

    private void configurarTabelaHistorico(){
        historyTable.getColumns().clear();
        var c1=new TableColumn<Transacao,String>("Data/Hora");
        c1.setCellValueFactory(c->new SimpleStringProperty(
                c.getValue().getDataHora()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        ));
        var c2=new TableColumn<Transacao,String>("Ativo");
        c2.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getMoeda().getSimbolo()));
        var c3=new TableColumn<Transacao,String>("Qtd");
        c3.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getQuantidade().toPlainString()));
        var c4=new TableColumn<Transacao,String>("Total (€)");
        c4.setCellValueFactory(c->new SimpleStringProperty(c.getValue().getTotalEur().toPlainString()));
        historyTable.getColumns().setAll(c1,c2,c3,c4);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void carregarHistorico(){
        int uid=SessaoAtual.utilizadorId;
        var lst=transacaoRepo.listarPorUsuario(uid);
        historyTable.setItems(FXCollections.observableArrayList(lst));
    }

    private void executar(String tipo){
        try{
            BigDecimal qtd=new BigDecimal(quantityField.getText().trim());
            BigDecimal price=marketToggle.isSelected()
                    ? ativoAtual.getValorAtual()
                    : new BigDecimal(priceField.getText().trim());

            // montar e persistir ordem
            Ordem ord=new Ordem();
            ord.setMoeda(ativoAtual);
            ord.setUtilizador(new model.Utilizador(){{setIdUtilizador(SessaoAtual.utilizadorId);}});
            ord.setQuantidade(qtd);
            ord.setPrecoUnitarioEur(price);
            ord.setDataCriacao(LocalDateTime.now());
            ord.setDataExpiracao(LocalDateTime.now().plusHours(24));
            ord.setIdTipoOrdem(ordemRepo.obterIdTipoOrdem(tipo));
            ord.setModo(marketToggle.isSelected()?"market":"limit");
            ord.setIdModo(ordemRepo.obterIdModo(ord.getModo()));
            ord.setIdStatus(ordemRepo.obterIdStatus("ativa"));

            // débito ou débito de crypto…
            var wr=Repository.WalletRepository.getInstance();
            var pr=new PortfolioRepository();
            if(tipo.equals("compra")){
                BigDecimal custo=price.multiply(qtd);
                if(wr.getSaldo(SessaoAtual.utilizadorId).compareTo(custo)<0||!wr.withdraw(SessaoAtual.utilizadorId,custo)){
                    new Alert(Alert.AlertType.ERROR,"Saldo insuficiente").show();return;
                }
            } else {
                if(pr.getQuantidade(SessaoAtual.utilizadorId,ativoAtual.getIdMoeda()).compareTo(qtd)<0||
                        !pr.diminuirQuantidade(SessaoAtual.utilizadorId,ativoAtual.getIdMoeda(),qtd)){
                    new Alert(Alert.AlertType.ERROR,"Crypto insuficiente").show();return;
                }
            }

            ord.setIdOrdem(ordemRepo.inserirOrdem(ord).orElseThrow());
            TradeService svc=new TradeService(conn);
            if(tipo.equals("compra")) svc.processarOrdemCompra(ord);
            else svc.processarOrdemVenda(ord);

            carregarOrdens();
            new Alert(Alert.AlertType.INFORMATION,"Ordem executada").show();
        } catch(Exception e){
            new Alert(Alert.AlertType.ERROR,"Erro: "+e.getMessage()).show();
        }
    }

    private LineChart<String,Number> criarChartParaMoeda(int id){
        CategoryAxis x=new CategoryAxis();
        NumberAxis y=new NumberAxis();
        x.setLabel("Hora"); y.setLabel("Preço");
        LineChart<String,Number> c=new LineChart<>(x,y);
        c.setAnimated(false); c.setLegendVisible(false); c.setCreateSymbols(false);
        var series=new XYChart.Series<String,Number>();
        var m=MarketSimulator.getMoedasSimuladas().get(id);
        series.getData().add(new XYChart.Data<>(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                m.getValorAtual()
        ));
        c.getData().add(series);
        FadeTransition ft=new FadeTransition(Duration.millis(800),c);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        return c;
    }
}
