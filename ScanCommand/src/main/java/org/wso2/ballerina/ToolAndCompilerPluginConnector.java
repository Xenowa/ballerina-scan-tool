package org.wso2.ballerina;

public abstract class ToolAndCompilerPluginConnector {
    private String messageFromTool;

    public void sendMessageFromTool(String messageFromTool) {
        System.out.println("Compiler plugin and Tool connected!");
        this.messageFromTool = messageFromTool;
    }

    public String getMessageFromTool() {
        return messageFromTool;
    }
}
