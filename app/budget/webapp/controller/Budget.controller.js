sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, JSONModel, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("expense.tracker.budget.controller.Budget", {
        onInit: function () {
            var oModel = new JSONModel({
                amount: 0,
                currency: "AUD",
                totalBudget: 0,
                spentAmount: 0,
                remainingAmount: 0,
                budgetUtilization: 0
            });
            this.getView().setModel(oModel);
            this._loadInitialBudget();
            this._loadBudgetSummary();
        },

        _loadBudgetSummary: function () {
            var oODataModel = this.getOwnerComponent().getModel();
            var oListBinding = oODataModel.bindList("/BudgetSummary");

            oListBinding.requestContexts(0, 1).then(function (aContexts) {
                if (aContexts.length > 0) {
                    var oData = aContexts[0].getObject();
                    var oViewModel = this.getView().getModel();
                    oViewModel.setProperty("/totalBudget", oData.totalBudget || 0);
                    oViewModel.setProperty("/currency", oData.currency || "AUD");
                    oViewModel.setProperty("/spentAmount", oData.spentAmount || 0);
                    oViewModel.setProperty("/remainingAmount", oData.remainingAmount || 0);
                    oViewModel.setProperty("/budgetUtilization", oData.budgetUtilization || 0);
                }
            }.bind(this)).catch(function (oError) {
                // BudgetSummary is available to all users, so this shouldn't fail
                MessageToast.show("Could not load budget summary");
            });
        },

        _loadInitialBudget: function () {
            var oODataModel = this.getOwnerComponent().getModel();
            var oListBinding = oODataModel.bindList("/Budget");

            oListBinding.requestContexts(0, 1).then(function (aContexts) {
                if (aContexts.length > 0) {
                    var oData = aContexts[0].getObject();
                    this.getView().getModel().setProperty("/amount", oData.amount);
                }
            }.bind(this)).catch(function (oError) {
                MessageBox.error(oError.message);
            });
        },

        onSave: function () {
            var oView = this.getView();
            var oModel = oView.getModel();
            var fAmount = oModel.getProperty("/amount");

            if (!fAmount || fAmount <= 0) {
                MessageBox.error("Please enter a valid budget amount greater than zero.");
                return;
            }

            var oODataModel = this.getOwnerComponent().getModel();
            var oListBinding = oODataModel.bindList("/Budget");

            oListBinding.requestContexts(0, 1).then(function (aContexts) {
                if (aContexts.length > 0) {
                    var oContext = aContexts[0];
                    oContext.setProperty("amount", fAmount);
                    oODataModel.submitBatch("$auto").then(function () {
                        MessageToast.show("Budget saved successfully!");
                        this._loadBudgetSummary();
                    }.bind(this));
                } else {
                    // Create a new budget if one doesn't exist
                    var oNewBudgetData = {
                        amount: fAmount,
                        description: "Overall Budget"
                    };

                    oListBinding.create(oNewBudgetData);
                    oODataModel.submitBatch("$auto").then(function () {
                        MessageToast.show("Budget created and saved successfully!");
                        this._loadBudgetSummary();
                    }.bind(this));
                }
            }.bind(this)).catch(function (oError) {
                MessageBox.error(oError.message);
            });
        }
    });
});
