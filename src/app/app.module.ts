import { LoginsuService } from './Services/loginsu.service';
import { Angular2SocialLoginService } from './Services/login.service';
import { AuthService } from './Services/auth.service';
import { SocialsignedinComponent } from './login/socialsignedin/socialsignedin.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { InitialComponent } from './login/initial/initial.component';
import { BrowserModule } from '@angular/platform-browser';
import { ErrorHandler, NgModule } from '@angular/core';
import { IonicApp, IonicErrorHandler, IonicModule } from 'ionic-angular';
import { SplashScreen } from '@ionic-native/splash-screen';
import { StatusBar } from '@ionic-native/status-bar';

import { MyApp } from './app.component';
import { HomePage } from '../pages/home/home';

@NgModule({
  declarations: [
    MyApp,
    HomePage,
    LoginComponent,
    HomeComponent,
    InitialComponent,
    SocialsignedinComponent,
    AuthService,
    Angular2SocialLoginService,
    LoginsuService,

    
  ],
  imports: [
    BrowserModule,
    IonicModule.forRoot(MyApp)
  ],
  bootstrap: [IonicApp],
  entryComponents: [
    MyApp,
    HomePage
  ],
  providers: [
    StatusBar,
    SplashScreen,
    AuthService,
    Angular2SocialLoginService,
    LoginsuService,
    {provide: ErrorHandler, useClass: IonicErrorHandler}
  ]
})
export class AppModule {}
