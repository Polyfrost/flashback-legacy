package com.moulberry.flashback.editor.ui;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import imgui.moulberry90.ImDrawData;
import imgui.moulberry90.ImFontAtlas;
import imgui.moulberry90.ImVec4;
import imgui.moulberry90.type.ImInt;
import org.lwjgl.system.MemoryUtil;
import org.polyfrost.flashbacklegacy.shim.com.mojang.blaze3d.pipeline.RenderTarget;
import org.polyfrost.flashbacklegacy.shim.com.mojang.renderpearl.api.textures.GpuTextureView;
import org.polyfrost.flashbacklegacy.shim.net.minecraft.client.renderer.texture.DynamicTexture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.*;

public class CustomImGuiImplB3D implements CustomImGuiRenderer {
    private static final int VERT_SIZE = 20;
    private int fontTexture;
    private ByteBuffer vertices = ByteBuffer.allocateDirect(0);
    private ByteBuffer indices = ByteBuffer.allocateDirect(0);

    @Override
    public void init() {
        ReplayUI.getIO().setBackendRendererName("flashback_legacy_gl2");
        updateFontsTexture();
    }

    @Override
    public long getTextureId(GpuTextureView view) {
        return view instanceof DynamicTexture.GlView gl ? gl.id() : 0L;
    }

    @Override
    public void setSampleLinear(long id) {
        setFilter(id, GL_LINEAR);
    }

    @Override
    public void setSampleNearest(long id) {
        setFilter(id, GL_NEAREST);
    }

    private static void setFilter(long id, int filter) {
        if (id <= 0) return;
        int old = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, (int) id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glBindTexture(GL_TEXTURE_2D, old);
    }

    @Override
    public void updateFontsTexture() {
        ImFontAtlas atlas = ReplayUI.getIO().getFonts();
        ImInt width = new ImInt();
        ImInt height = new ImInt();
        ByteBuffer pixels = atlas.getTexDataAsRGBA32(width, height);
        if (fontTexture == 0) fontTexture = glGenTextures();
        int old = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glBindTexture(GL_TEXTURE_2D, old);
        atlas.setTexID(fontTexture);
    }

    @Override
    public RenderTarget renderDrawData(ImDrawData data) {
        int fbWidth = (int) (data.getDisplaySizeX() * data.getFramebufferScaleX());
        int fbHeight = (int) (data.getDisplaySizeY() * data.getFramebufferScaleY());
        int lists = data.getCmdListsCount();
        if (fbWidth <= 0 || fbHeight <= 0 || lists == 0) return null;

        int idxSize = ImDrawData.sizeOfImDrawIdx();
        vertices = fill(vertices, data.getTotalVtxCount() * VERT_SIZE, lists, data, true);
        indices = fill(indices, data.getTotalIdxCount() * idxSize, lists, data, false);
        long vtxBase = MemoryUtil.memAddress(vertices);
        long idxBase = MemoryUtil.memAddress(indices);

        int program = glGetInteger(GL_CURRENT_PROGRAM);
        int arrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        int elementBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        int framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
        int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        int clientTexture = glGetInteger(GL_CLIENT_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        glClientActiveTexture(GL_TEXTURE0);
        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT | GL_SCISSOR_BIT
                | GL_VIEWPORT_BIT | GL_CURRENT_BIT | GL_POLYGON_BIT | GL_LIGHTING_BIT | GL_DEPTH_BUFFER_BIT);
        glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);
        int texture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        float left = data.getDisplayPosX(), top = data.getDisplayPosY();
        glOrtho(left, left + data.getDisplaySizeX(), top + data.getDisplaySizeY(), top, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glUseProgram(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, fbWidth, fbHeight);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_LIGHTING);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_FOG);
        glEnable(GL_SCISSOR_TEST);
        glEnable(GL_TEXTURE_2D);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glShadeModel(GL_SMOOTH);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glDisableClientState(GL_NORMAL_ARRAY);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);

        int indexType = idxSize == 2 ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT;
        float scaleX = data.getFramebufferScaleX(), scaleY = data.getFramebufferScaleY();
        ImVec4 clip = new ImVec4();
        long vtxOffset = 0, idxOffset = 0;
        for (int list = 0; list < lists; list++) {
            long vtx = vtxBase + vtxOffset * VERT_SIZE;
            glVertexPointer(2, GL_FLOAT, VERT_SIZE, vtx);
            glTexCoordPointer(2, GL_FLOAT, VERT_SIZE, vtx + 8);
            glColorPointer(4, GL_UNSIGNED_BYTE, VERT_SIZE, vtx + 16);
            int commands = data.getCmdListCmdBufferSize(list);
            for (int cmd = 0; cmd < commands; cmd++) {
                data.getCmdListCmdBufferClipRect(clip, list, cmd);
                int minX = Math.max(0, (int) ((clip.x - left) * scaleX));
                int minY = Math.max(0, (int) ((clip.y - top) * scaleY));
                int maxX = Math.min(fbWidth, (int) ((clip.z - left) * scaleX));
                int maxY = Math.min(fbHeight, (int) ((clip.w - top) * scaleY));
                if (maxX <= minX || maxY <= minY) continue;
                glScissor(minX, fbHeight - maxY, maxX - minX, maxY - minY);
                glBindTexture(GL_TEXTURE_2D, (int) data.getCmdListCmdBufferTextureId(list, cmd));
                long first = idxOffset + data.getCmdListCmdBufferIdxOffset(list, cmd);
                glDrawElements(GL_TRIANGLES, data.getCmdListCmdBufferElemCount(list, cmd), indexType, idxBase + first * idxSize);
            }
            vtxOffset += data.getCmdListVtxBufferSize(list);
            idxOffset += data.getCmdListIdxBufferSize(list);
        }

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_TEXTURE);
        glPopMatrix();
        glBindTexture(GL_TEXTURE_2D, texture);
        glPopClientAttrib();
        glPopAttrib();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
        glUseProgram(program);
        glClientActiveTexture(clientTexture);
        glActiveTexture(activeTexture);
        return null;
    }

    private static ByteBuffer fill(ByteBuffer buffer, int bytes, int lists, ImDrawData data, boolean vertices) {
        if (buffer.capacity() < bytes) buffer = ByteBuffer.allocateDirect(bytes + 8192).order(ByteOrder.nativeOrder());
        buffer.clear();
        for (int i = 0; i < lists; i++) buffer.put(vertices ? data.getCmdListVtxBufferData(i) : data.getCmdListIdxBufferData(i));
        buffer.flip();
        return buffer;
    }
}
