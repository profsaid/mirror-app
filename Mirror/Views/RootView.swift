import SwiftUI

struct RootView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var model = model
        TabView(selection: $model.tab) {
            HomeView()
                .tabItem { Label("Home", systemImage: "house") }
                .tag(AppTab.home)
            StylesView()
                .tabItem { Label("Styles", systemImage: "scissors") }
                .tag(AppTab.styles)
            SavedView()
                .tabItem { Label("Saved", systemImage: "heart") }
                .tag(AppTab.saved)
        }
        .fullScreenCover(isPresented: $model.showCapture) {
            CaptureView()
                .environment(model)
        }
    }
}
