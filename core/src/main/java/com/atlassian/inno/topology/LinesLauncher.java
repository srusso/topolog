package com.atlassian.inno.topology;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class LinesLauncher extends ApplicationAdapter {

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private float angle;
    String vertexShader = "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "v_color = vec4(1, 1, 1, 1);\n" +
        "v_texCoords = a_texCoord0;\n" +
        "gl_Position =  u_projTrans * a_position;\n" +
        "}";

    String fragmentShader = "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec4 v_color;\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "void main() {\n" +
        "gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
        "}";

    ShaderProgram shaderProgram;

    private Mesh lineMesh;

    Matrix4 meshOrientation = new Matrix4();

    @Override
    public void create() {
        // Set up the camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(3f, 3f, 3f);
        camera.lookAt(0, 0, 0);
        camera.near = 1f;
        camera.far = 300f;
        camera.update();

        shaderProgram = new ShaderProgram(vertexShader, fragmentShader);

        // Set up the model batch
        modelBatch = new ModelBatch();

        // Set up the environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));

        createCurvyLineMesh();

        angle = 0f;
    }

    int o = 0;

    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Rotate the cube
        angle += Gdx.graphics.getDeltaTime() * 0.05f;
        if (o++ % 100 == 0) {
            System.out.println(angle);
        }

        meshOrientation = meshOrientation.setToRotationRad(Vector3.Y, angle);
//        shaderProgram.bind();
//        shaderProgram.setUniformMatrix("u_projTrans", camera.combined);
        lineMesh.transform(meshOrientation);
        lineMesh.render(
            shaderProgram,
            GL20.GL_LINES);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        lineMesh.dispose();
        shaderProgram.dispose();
    }

    private void createCurvyLineMesh() {
        float[] verts = new float[20];
        int i = 0;

        verts[i++] = -1; // x1
        verts[i++] = -1; // y1
        verts[i++] = 0;
        verts[i++] = 0f; // u1
        verts[i++] = 0f; // v1

        verts[i++] = 1f; // x2
        verts[i++] = -1; // y2
        verts[i++] = 0;
        verts[i++] = 1f; // u2
        verts[i++] = 0f; // v2

        verts[i++] = 1f; // x3
        verts[i++] = 1f; // y3
        verts[i++] = 0;
        verts[i++] = 1f; // u3
        verts[i++] = 1f; // v3

        verts[i++] = -1; // x4
        verts[i++] = 1f; // y4
        verts[i++] = 0;
        verts[i++] = 0f; // u4
        verts[i++] = 1f; // v4

        for (int u = 0 ; u < verts.length ; u++) {
            verts[u] = verts[u] * 0.9f;
        }

        lineMesh = new Mesh(true, 4, 0,  // static mesh with 4 vertices and no indices
            new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        lineMesh.setVertices(verts);
    }


}
