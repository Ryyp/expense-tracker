sap.ui.define([
    "sap/ui/core/mvc/ControllerExtension",
    "sap/ui/model/json/JSONModel",
    "sap/ui/core/Fragment",
    "sap/f/DynamicPageHeader"
], function (ControllerExtension, JSONModel, Fragment, DynamicPageHeader) {
    "use strict";

    return ControllerExtension.extend("expense.tracker.transactions.ext.controller.ListReportExt", {

        override: {
            onInit: function () {
                this._initializeBudgetSummaryModel();
                this._attachRouterEvents();
            },

            onAfterRendering: function () {
                this._addBudgetHeaderToPage();
                this._loadBudgetSummary();
            },

            editFlow: {
                onAfterDelete: function() {
                    this._refreshBudget("delete");
                },

                onAfterCreate: function() {
                    this._refreshBudget("create");
                },

                onAfterSave: function() {
                    this._refreshBudget("save");
                }
            }
        },

        /**
         * Refresh budget summary with logging
         * @param {string} sOperation - The operation that triggered the refresh
         * @private
         */
        _refreshBudget: function(sOperation) {
            console.log(`Budget refresh triggered by: ${sOperation}`);
            this._loadBudgetSummary();
        },

        /**
         * Initialize the budget summary JSON model
         * @private
         */
        _initializeBudgetSummaryModel: function () {
            const oBudgetModel = new JSONModel({
                totalBudget: 0,
                currency: "AUD",
                spentAmount: 0,
                remainingAmount: 0,
                budgetUtilization: 0
            });
            this.base.getView().setModel(oBudgetModel, "budgetSummary");
        },

        /**
         * Attach router events for navigation refresh
         * @private
         */
        _attachRouterEvents: function () {
            const oRouter = this.base.getAppComponent().getRouter();
            oRouter?.getRoute("TransactionsList")?.attachMatched(() => {
                this._refreshBudget("navigation");
            });
        },

        /**
         * Add budget summary fragment to the DynamicPage header
         * @private
         */
        _addBudgetHeaderToPage: function () {
            const oView = this.base.getView();
            const oDynamicPage = oView.getContent()[0];

            if (!oDynamicPage) {
                console.error("DynamicPage not found");
                return;
            }

            Fragment.load({
                id: oView.getId(),
                name: "expense.tracker.transactions.ext.fragment.BudgetSummaryCards",
                controller: this
            }).then((oFragment) => {
                this._insertFragmentIntoHeader(oDynamicPage, oFragment);
            }).catch((oError) => {
                console.error("Failed to load budget summary fragment:", oError);
            });
        },

        /**
         * Insert the fragment into the DynamicPage header
         * @param {sap.f.DynamicPage} oDynamicPage - The DynamicPage control
         * @param {sap.ui.core.Control} oFragment - The fragment to insert
         * @private
         */
        _insertFragmentIntoHeader: function (oDynamicPage, oFragment) {
            let oHeader = oDynamicPage.getHeader();

            if (oHeader) {
                oHeader.insertContent(oFragment, 0);
            } else {
                oDynamicPage.setHeader(new DynamicPageHeader({
                    content: [oFragment]
                }));
            }
        },

        /**
         * Load budget summary data from the OData service
         * @private
         */
        _loadBudgetSummary: function () {
            const oModel = this.base.getView().getModel();

            if (!oModel) {
                console.error("OData model not available");
                return;
            }

            oModel.bindList("/BudgetSummary")
                .requestContexts(0, 1)
                .then((aContexts) => {
                    if (aContexts.length > 0) {
                        this._updateBudgetSummaryModel(aContexts[0].getObject());
                    }
                })
                .catch((oError) => {
                    console.error("Failed to load budget summary:", oError);
                });
        },

        /**
         * Update the budget summary model with loaded data
         * @param {Object} oData - The budget summary data
         * @private
         */
        _updateBudgetSummaryModel: function (oData) {
            const oBudgetModel = this.base.getView().getModel("budgetSummary");

            oBudgetModel.setData({
                totalBudget: oData.totalBudget || 0,
                currency: oData.currency || "AUD",
                spentAmount: oData.spentAmount || 0,
                remainingAmount: oData.remainingAmount || 0,
                budgetUtilization: oData.budgetUtilization || 0
            });

            console.log("Budget updated:", oData);
        }
    });
});
